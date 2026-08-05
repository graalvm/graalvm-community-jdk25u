/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, 2026, IBM Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.svm.hosted.code;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.oracle.graal.pointsto.flow.AnalysisParsedGraph;
import com.oracle.svm.core.util.VMError;
import com.oracle.svm.hosted.meta.HostedMethod;
import jdk.graal.compiler.bytecode.BytecodeProvider;
import jdk.graal.compiler.nodes.EncodedGraph;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.nodes.ValueNode;
import jdk.graal.compiler.nodes.graphbuilderconf.InlineInvokePlugin;
import jdk.graal.compiler.phases.contract.NodeCostUtil;
import jdk.graal.compiler.phases.util.Providers;
import jdk.graal.compiler.replacements.PEGraphDecoder;
import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * This decoder is used after trivial inlining. It accounts for expected optimization benefit when
 * making inlining decisions. Callees are "trialed" by first being inlined before a decision is
 * made. This allows expected cost and benefit to be computed. Benefit is awarded based on
 * successful local optimizations (canonicalization). Benefit is awarded in
 * {@link jdk.graal.compiler.nodes.SimplifyingGraphDecoder}. If inlining is denied, then the nodes
 * moved into the caller are rolled back, similar to what is done in the
 * {@link com.oracle.graal.pointsto.phases.InlineBeforeAnalysisGraphDecoder}.
 *
 * <p>
 * This inliner implementation is a variation of the one described in the paper "An
 * Optimization-Driven Incremental Inline Substitution Algorithm for Just-in-Time Compilers" (DOI:
 * <a href="https://doi.org/10.1109/CGO.2019.8661171">10.1109/CGO.2019.8661171</a>). This
 * implementation has some differences:
 * <ol>
 * <li>The benefit of some optimizations that reduce graph size (remove nodes or conditionals) are
 * weighted more heavily.</li>
 * <li>T1 and T2 constant factors in the threshold function were chosen to be different.</li>
 * <li>Method call frequency cannot be used.</li>
 * <li>Each callee is evaluated independently instead of sharing a budget with other callees
 * belonging to the same root method. This is achieved by excluding the root size term from the
 * threshold function. The reason for this is to avoid needing to use prioritization to order the
 * evaluation of callees.</li>
 * </ol>
 */
class NonTrivialInliningGraphDecoder extends PEGraphDecoder {

    /*
     * These threshold function constants are different from those chosen in
     * "An Optimization-Driven Incremental Inline Substitution Algorithm for Just-in-Time Compilers"
     * (t1=0.005, t2= 120). These values were chosen empirically. Both t1 and t2 are more strict
     * than in the aforementioned paper for a few reasons: 1. Some optimizations are given an extra
     * heavy benefit weighting. This results in a higher benefit|cost. 2. Only the callee size, not
     * the root size is accounted for in the threshold function. This is so that each callee may be
     * evaluated independently without a shared budget, which would require callee prioritization to
     * correctly order inlining trials. This results in a lower threshold. 3. Each callee is
     * evaluated independently. There is a per-callee budget, not a shared budget. All of these
     * reasons result in needing a threshold that is harder to overcome.
     */
    private static final double T1 = 5;
    private static final double T2 = 1;

    boolean inlinedDuringDecoding;
    int round;

    NonTrivialInliningGraphDecoder(StructuredGraph graph, Providers providers, com.oracle.svm.hosted.code.CompileQueue.NonTrivialInliningPlugin inliningPlugin, int round) {
        super(AnalysisParsedGraph.HOST_ARCHITECTURE, graph, providers, null,
                        null,
                        new InlineInvokePlugin[]{inliningPlugin},
                        null, null, null, null,
                        new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), true, false);
        this.round = round;
    }

    @Override
    protected EncodedGraph lookupEncodedGraph(ResolvedJavaMethod method, BytecodeProvider intrinsicBytecodeProvider) {
        return ((HostedMethod) method).compilationInfo.getCompilationGraph().getEncodedGraph();
    }


    /**
     * Calculate the size before inlining. It will be used later to calculate the callee cost when
     * making inlining decisions.
     */
    @Override
    protected LoopScope doInline(PEMethodScope methodScope, LoopScope loopScope, InvokeData invokeData, InlineInvokePlugin.InlineInfo inlineInfo, ValueNode[] arguments) {
        // First, get the root method
        PEMethodScope scope = methodScope;
        while (scope.caller != null) {
            scope = scope.caller;
        }
        HostedMethod root = (HostedMethod) scope.method;
        HostedMethod callee = (HostedMethod) inlineInfo.getMethodToInline();
        // If needed, create a CalleeInfo for the current callee
        CalleeInfo calleeInfo = root.compilationInfo.callees.get(callee);
        if (calleeInfo == null) {
            // If this callee is inlined, this CalleeInfo will not survive beyond the current round.
            calleeInfo = new CalleeInfo(callee, round);
            root.compilationInfo.callees.put(callee, calleeInfo);
        }
        /*
         * Stash the graph size in the CalleeInfo. Recursion (due to multiple callsites at different
         * depths) should not be a problem since we only go one level deep per round.
         */
        calleeInfo.sizeBeforeInlining = NodeCostUtil.computeGraphSize(graph);
        return super.doInline(methodScope, loopScope, invokeData, inlineInfo, arguments);
    }

    private boolean canInline(PEMethodScope inlineScope, HostedMethod root, HostedMethod callee) {
        if (callee.shouldBeInlined()) {
            return true;
        }
        VMError.guarantee(inlineScope.caller.caller == null, "Inliner should not be evaluating beyond the root's immediate callees (unless forced by annotations).");

        CalleeInfo calleeInfo = root.compilationInfo.callees.get(callee);
        VMError.guarantee(calleeInfo != null, "CalleeInfo should have been created in doInline");

        double currentSize = NodeCostUtil.computeGraphSize(graph);
        double calleeCost = (currentSize - calleeInfo.sizeBeforeInlining);

        double offset = 1.0;
        double bc = (offset + inlineScope.benefit) / calleeCost;

        double threshold = T1 * Math.pow(2, (calleeCost / (16 * T2)));
        if (bc >= threshold) {
            return true;
        }
        /*
         * If we fail to inline, the CalleeInfo remains in the root's set, so we don't retrial it in
         * future rounds unless it changes.
         */
        return false;
    }

    @Override
    protected void finishInlining(MethodScope is) {
        PEMethodScope inlineScope = (PEMethodScope) is;
        PEMethodScope callerScope = inlineScope.caller;
        HostedMethod callee = (HostedMethod) inlineScope.method;
        LoopScope callerLoopScope = inlineScope.callerLoopScope;
        InvokeData invokeData = inlineScope.invokeData;
        HostedMethod root = (HostedMethod) callerScope.method;

        if (!canInline(inlineScope, root, callee)) {
            undoInlining(inlineScope, callerScope, callerLoopScope, invokeData);
            return;
        }

        // Remove callee from the "seen" set
        root.compilationInfo.callees.remove(callee);

        inlinedDuringDecoding = true;
        super.finishInlining(inlineScope);
    }
}