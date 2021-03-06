package cz.agents.gtlibrary.algorithms.stackelberg.iterativelp.bfs;

import cz.agents.gtlibrary.algorithms.sequenceform.refinements.LPData;
import cz.agents.gtlibrary.algorithms.stackelberg.iterativelp.SumForbiddingStackelbergLP;
import cz.agents.gtlibrary.interfaces.GameInfo;
import cz.agents.gtlibrary.interfaces.InformationSet;
import cz.agents.gtlibrary.interfaces.Player;
import cz.agents.gtlibrary.interfaces.Sequence;
import cz.agents.gtlibrary.utils.Pair;
import cz.agents.gtlibrary.utils.Triplet;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.util.*;

public class BFSForbiddingStackelbergLP extends SumForbiddingStackelbergLP {
    public BFSForbiddingStackelbergLP(Player leader, GameInfo info) {
        super(leader, info);
    }

    @Override
    protected Pair<Map<Sequence, Double>, Double> handleBrokenStrategyCause(double lowerBound, double upperBound, LPData lpData, double value, Iterable<Sequence> brokenStrategyCauses) {
        Pair<Map<Sequence, Double>, Double> currentBest = dummyResult;
        Queue<Triplet<Iterable<Sequence>, Sequence, Double>> queue = new PriorityQueue<>(new Comparator<Triplet<Iterable<Sequence>, Sequence, Double>>() {
            @Override
            public int compare(Triplet<Iterable<Sequence>, Sequence, Double> o1, Triplet<Iterable<Sequence>, Sequence, Double> o2) {
                return Double.compare(o2.getThird(), o1.getThird());
            }
        });

//        current.setValue(reward);
        for (Sequence brokenStrategyCause : brokenStrategyCauses) {
            restrictFollowerPlay(brokenStrategyCause, brokenStrategyCauses, lpData);
//            currentSequences.add(brokenStrategyCause);
//            double bestFit = getBestFit(initSet, lowerBound);
//
//            System.err.println("best fit: " + bestFit + ". lower bound: " + lowerBound);
//            if (bestFit <= lowerBound) {
//                System.err.println("duplicity cut");
//                return dummyResult;
//            }
//            SequenceSet temp = current;
//
//            current = current.createSuperSet(brokenStrategyCause, reward);
            Pair<Iterable<Sequence>, Double> result = probe();

            if (result.getRight() > Double.NEGATIVE_INFINITY) {
//                current.setValue(result.getRight());
                if (result.getLeft() == null) {
                    if (lowerBound <= result.getRight()) {
                        lowerBound = result.getRight();
                        if (result.getRight() > currentBest.getRight())
                            currentBest = new Pair<>(null, result.getRight());
                    }
                } else {
                    if (result.getRight() > lowerBound)
                        queue.add(new Triplet<>(result.getLeft(), brokenStrategyCause, result.getRight()));
                }
            }
//            current = temp;
//            currentSequences.remove(brokenStrategyCause);
            removeRestriction(brokenStrategyCause, brokenStrategyCauses, lpData);
        }
        while (!queue.isEmpty()) {
            Triplet<Iterable<Sequence>, Sequence, Double> current = queue.poll();

//            currentSequences.add(current.getSecond());
//            double bestFit = getBestFit(initSet, getLowerBound(lowerBound, currentBest));
//
//            System.err.println("best fit: " + bestFit + ". lower bound: " + getLowerBound(lowerBound, currentBest));
//            if (bestFit <= lowerBound) {
//                System.err.println("duplicity cut");
//                return dummyResult;
//            }
//            SequenceSet temp = this.current;
//
//            this.current = this.current.createSuperSet(current.getSecond(), current.getThird() == Double.NEGATIVE_INFINITY ? reward : current.getThird());
            restrictFollowerPlay(current.getSecond(), brokenStrategyCauses, lpData);
            if (current.getThird() > lowerBound)
                for (Sequence brokenStrategyCause : current.getFirst()) {
                    restrictFollowerPlay(brokenStrategyCause, current.getFirst(), lpData);
                    lowerBound = getLowerBound(lowerBound, currentBest);
//                    currentSequences.add(brokenStrategyCause);
//                    bestFit = getBestFit(initSet, lowerBound);
//
//                    System.err.println("best fit: " + bestFit + ". lower bound: " + getLowerBound(lowerBound, currentBest));
//                    if (bestFit <= lowerBound) {
//                        System.err.println("duplicity cut");
//                        return dummyResult;
//                    }
//                    SequenceSet tempTemp = this.current;
//
//                    this.current = this.current.createSuperSet(brokenStrategyCause, current.getThird() == Double.NEGATIVE_INFINITY ? reward : current.getThird());

                    Pair<Map<Sequence, Double>, Double> result = solve(lowerBound, upperBound);

//                    this.current.setValue(result.getRight());
                    if (result.getRight() > currentBest.getRight()) {
                        currentBest = result;
                        if (currentBest.getRight() >= value - eps) {
//                            System.out.println("----------------currentBest " + currentBest.getRight() + " reached parent reward " + reward + "----------------");
                            return currentBest;
                        }
                    }
//                    this.current = tempTemp;
//                    currentSequences.remove(brokenStrategyCause);
                    removeRestriction(brokenStrategyCause, current.getFirst(), lpData);
                }
//            this.current = temp;
//            currentSequences.remove(current.getSecond());
            removeRestriction(current.getSecond(), brokenStrategyCauses, lpData);
        }
        return currentBest;
    }

    protected Pair<Iterable<Sequence>, Double> probe() {
        try {
            long startTime = threadBean.getCurrentThreadCpuTime();
            LPData lpData = lpTable.toCplex();

            overallConstraintGenerationTime += threadBean.getCurrentThreadCpuTime() - startTime;
//            lpData.getSolver().exportModel("SSEIter.lp");
            startTime = threadBean.getCurrentThreadCpuTime();
            lpData.getSolver().solve();
            lpInvocationCount++;
            overallConstraintLPSolvingTime += threadBean.getCurrentThreadCpuTime() - startTime;
            printBinaryVariableValues(lpData);
            if (lpData.getSolver().getStatus() == IloCplex.Status.Optimal) {
                double value = lpData.getSolver().getObjValue();

                System.out.println("**************");
                System.out.println("prober LP reward: " + value);
//                for (Map.Entry<Object, IloNumVar> entry : lpData.getWatchedPrimalVariables().entrySet()) {
//                    if (entry.getKey() instanceof Pair && ((Pair) entry.getKey()).getLeft().equals("v")) {
//                        double variableValue = lpData.getSolver().getValue(entry.getValue());
//
//                        if (variableValue != 0)
//                            System.out.println(entry.getKey() + ": " + variableValue);
//                    }
//                }
                Map<Sequence, Double> leaderRealPlan = behavioralToRealizationPlan(getLeaderBehavioralStrategy(lpData, leader));
                Pair<Map<Sequence, Double>, Double> result = followerBestResponse.computeBestResponseTo(leaderRealPlan);

                if(Math.abs(result.getRight() - value) < eps) {
                    System.out.println("solution found in probe BR");
                    return new Pair<>(null, value);
                }
                Map<InformationSet, Map<Sequence, Double>> behavStrat = getSequenceEvaluation(lpData, follower);
//                Iterable<Sequence> brokenStrategyCauses = getBrokenStrategyCauses(behavStrat);
//
//                System.out.println("follower behav. strat.");
//                for (Map.Entry<InformationSet, Map<Sequence, Double>> entry : behavStrat.entrySet()) {
//                    System.out.println(entry);
//                }
                return new Pair<>(getBrokenStrategyCauses(behavStrat, lpData), value);
            } else {
                System.err.println(lpData.getSolver().getStatus());
            }
        } catch (IloException e) {
            e.printStackTrace();
        }
        return new Pair<>(null, Double.NEGATIVE_INFINITY);
    }
}
