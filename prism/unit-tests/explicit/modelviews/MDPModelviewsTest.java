package explicit.modelviews;

import common.BitSetTools;
import common.functions.MappingInt;
import common.functions.PairMapping;
import common.functions.PairPredicateInt;
import common.iterable.IterableArray;
import explicit.*;
import org.junit.jupiter.api.Test;
import prism.PrismException;
import prism.PrismPrintStreamLog;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class MDPModelviewsTest {

    MDPSimple <Double> original1;
    MDPSimple <Double>  original2;

    public void init() throws PrismException {
        original1 = new MDPSimple(3);
        original1.addInitialState(0);
        Distribution<Double> dist = new Distribution();
        dist.add(1, 0.1);
        dist.add(2, 0.9);
        original1.addActionLabelledChoice(0, dist, "a");
        dist = new Distribution();
        dist.add(1, 0.2);
        dist.add(2, 0.8);
        original1.addActionLabelledChoice(0, dist, "b");
        dist = new Distribution();
        dist.add(1, 1.0);
        original1.addActionLabelledChoice(1, dist, "a");
        dist = new Distribution();
        dist.add(2, 1.0);
        original1.addActionLabelledChoice(1, dist, "b");

        original2 = new MDPSimple(2);
        original2.addInitialState(0);
        dist = new Distribution();
        dist.add(0, 0.1);
        dist.add(1, 0.9);
        original2.addActionLabelledChoice(0, dist, "a");
        dist = new Distribution();
        dist.add(0, 0.5);
        dist.add(1, 0.5);
        original2.addActionLabelledChoice(0, dist, "b");
        dist = new Distribution();
        dist.add(0, 1.0);
        original2.addActionLabelledChoice(1, dist, "a");

    }

    public String exportDotFileToString(Model model) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        model.exportToDotFile(new PrismPrintStreamLog(new PrintStream(outputStream)));
        byte[] buffer = outputStream.toByteArray();

        return new String(buffer);
    }

    // disjointUnion

    @Test
    public void disjointUnion() throws PrismException {
        init();

        MDP union;
        union = new MDPDisjointUnion(original1, original2);
        union.findDeadlocks(true);


        MDPSimple union2 = new MDPSimple(5);
        union2.addInitialState(0);
        Distribution<Double> dist = new Distribution();
        dist.add(1, 0.1);
        dist.add(2, 0.9);
        union2.addActionLabelledChoice(0, dist, "a");
        dist = new Distribution();
        dist.add(1, 0.2);
        dist.add(2, 0.8);
        union2.addActionLabelledChoice(0, dist, "b");
        dist = new Distribution();
        dist.add(1, 1.0);
        union2.addActionLabelledChoice(1, dist, "a");
        dist = new Distribution();
        dist.add(2, 1.0);
        union2.addActionLabelledChoice(1, dist, "b");
        dist = new Distribution();
        dist.add(2, 1.0);
        union2.addChoice(2, dist);
        dist = new Distribution();
        dist.add(3, 0.1);
        dist.add(4, 0.9);
        union2.addActionLabelledChoice(3, dist, "a");
        dist = new Distribution();
        dist.add(3, 0.5);
        dist.add(4, 0.5);
        union2.addActionLabelledChoice(3, dist, "b");
        dist = new Distribution();
        dist.add(3, 1.0);
        union2.addActionLabelledChoice(4, dist, "a");

        assertEquals(exportDotFileToString(union2), exportDotFileToString(union));
    }

    @Test
    public void disjointUnionEmptyModel() throws PrismException {
        init();

        original1 = new MDPSimple(3);

        MDP union;
        union = new MDPDisjointUnion(original1, original2);
        union.findDeadlocks(true);


        MDPSimple union2 = new MDPSimple(5);
        union2.addInitialState(0);
        Distribution<Double> dist = new Distribution();
        dist.add(0, 1.0);
        union2.addChoice(0, dist);
        dist = new Distribution();
        dist.add(1, 1.0);
        union2.addChoice(1, dist);
        dist = new Distribution();
        dist.add(2, 1.0);
        union2.addChoice(2, dist);
        dist = new Distribution();
        dist.add(3, 0.1);
        dist.add(4, 0.9);
        union2.addActionLabelledChoice(3, dist, "a");
        dist = new Distribution();
        dist.add(3, 0.5);
        dist.add(4, 0.5);
        union2.addActionLabelledChoice(3, dist, "b");
        dist = new Distribution();
        dist.add(3, 1.0);
        union2.addActionLabelledChoice(4, dist, "a");

        assertEquals(exportDotFileToString(union2), exportDotFileToString(union));
    }

    // additionalChoices

    @Test
    public void additionalChoices() throws PrismException {
        init();

        final MappingInt<List<Iterator<Map.Entry<Integer, Double>>>> choices = new MappingInt<List<Iterator<Map.Entry<Integer, Double>>>>() {
            @Override
            public List<Iterator<Map.Entry<Integer, Double>>> apply(final int state) {
                final Distribution distribution = new Distribution();
                switch (state) {
                    case 1:
                        distribution.set(2, 0.4);
                        distribution.set(3, 0.6);
                        break;
                    case 3:
                        distribution.set(2, 0.5);
                        distribution.set(3, 0.5);
                        break;
                    default:
                        return null;
                }
                return Collections.singletonList(distribution.iterator());
            }
        };

        final MappingInt<List<Object>> actions = new MappingInt<List<Object>>() {
            @Override
            public List<Object> apply(final int state) {
                final Object action;
                switch (state) {
                    case 1:
                        action = "d";
                        break;
                    case 3:
                        action = "e";
                        break;
                    default:
                        action = null;
                }
                return Collections.singletonList(action);
            }
        };

        MDP alteredChoices = new MDPAdditionalChoices(original1, choices, actions);
        alteredChoices.findDeadlocks(true);


        MDPSimple alteredChoices2 = new MDPSimple(3);
        alteredChoices2.addInitialState(0);
        Distribution<Double> dist = new Distribution();
        dist.add(1, 0.1);
        dist.add(2, 0.9);
        alteredChoices2.addActionLabelledChoice(0, dist, "a");
        dist = new Distribution();
        dist.add(1, 0.2);
        dist.add(2, 0.8);
        alteredChoices2.addActionLabelledChoice(0, dist, "b");
        dist = new Distribution();
        dist.add(1, 1.0);
        alteredChoices2.addActionLabelledChoice(1, dist, "a");
        dist = new Distribution();
        dist.add(2, 1.0);
        alteredChoices2.addActionLabelledChoice(1, dist, "b");
        dist = new Distribution();
        dist.add(2, 0.4);
        dist.add(3, 0.6);
        alteredChoices2.addActionLabelledChoice(1, dist, "d");
        dist = new Distribution();
        dist.add(2, 1.0);
        alteredChoices2.addChoice(2, dist);

        assertEquals(exportDotFileToString(alteredChoices2), exportDotFileToString(alteredChoices));
    }

    @Test
    public void droppedChoices() throws PrismException {
        init();
        final PairPredicateInt dropped = new PairPredicateInt() {
            @Override
            public boolean test(final int state, final int choice) {
                switch (state) {
                    case 0:
                        return choice == 1;
                    case 1:
                        return true;
                    default:
                        break;
                }
                return false;
            }
        };

        MDP droppedChoices = new MDPDroppedChoices(original1, dropped);
        droppedChoices.findDeadlocks(true);

        MDPSimple droppedChoices2 = new MDPSimple(3);
        droppedChoices2.addInitialState(0);
        Distribution<Double> dist = new Distribution();
        dist.add(1, 0.1);
        dist.add(2, 0.9);
        droppedChoices2.addActionLabelledChoice(0, dist, "a");
        dist = new Distribution();
        dist.add(1, 1.0);
        droppedChoices2.addChoice(1, dist);
        dist = new Distribution();
        dist.add(2, 1.0);
        droppedChoices2.addChoice(2, dist);

        assertEquals(exportDotFileToString(droppedChoices2), exportDotFileToString(droppedChoices));
    }

    @Test
    public void allDroppedChoices() throws PrismException {
        init();

        final BitSet dropped = BitSetTools.asBitSet(0, 2);


        MDP droppedChoices = new MDPDroppedAllChoices(original1, dropped);
        droppedChoices.findDeadlocks(true);

        MDPSimple droppedChoices2 = new MDPSimple(3);
        droppedChoices2.addInitialState(0);
        Distribution<Double> dist = new Distribution();
        dist.add(0, 1.0);
        droppedChoices2.addChoice(0, dist);
        dist = new Distribution();
        dist.add(1, 1.0);
        droppedChoices2.addActionLabelledChoice(1, dist, "a");
        dist = new Distribution();
        dist.add(2, 1.0);
        droppedChoices2.addActionLabelledChoice(1, dist, "b");
        dist = new Distribution();
        dist.add(2, 1.0);
        droppedChoices2.addChoice(2, dist);

        assertEquals(exportDotFileToString(droppedChoices2), exportDotFileToString(droppedChoices));
    }

    @Test
    public void alterdDistributions() throws PrismException {
        init();

        Distribution <Double>dist = new Distribution();
        dist.add(2, 1.0);
        original1.addChoice(2, dist);

        final PairMapping<Integer, Integer, Iterator<Map.Entry<Integer, Double>>> transitions = new PairMapping<Integer, Integer, Iterator<Map.Entry<Integer, Double>>>() {
            @Override
            public Iterator<Map.Entry<Integer, Double>> apply(final Integer state, final Integer choice) {
                final Distribution distribution = new Distribution();
                if (state == 1 && choice == 1) {
                    distribution.set(2, 0.4);
                    distribution.set(3, 0.6);
                } else if (state == 2) {
                    distribution.set(0, 0.25);
                    distribution.set(1, 0.25);
                    distribution.set(2, 0.25);
                    distribution.set(3, 0.25);
                } else {
                    return null;
                }
                return distribution.iterator();
            }
        };

        final PairMapping<Integer, Integer, Object> actions = new PairMapping<Integer, Integer, Object>() {
            @Override
            public Object apply(final Integer state, final Integer choice) {
                Object action = original1.getAction(state, choice);
                if (action instanceof String) {
                    return action + "'";
                } else {
                    return "d";
                }
            }
        };


        MDP alteredDistributions = new MDPAlteredDistributions(original1, transitions, actions);
        alteredDistributions.findDeadlocks(true);

        MDPSimple alteredDistributions2 = new MDPSimple(3);
        alteredDistributions2.addInitialState(0);
        dist = new Distribution();
        dist.add(1, 0.1);
        dist.add(2, 0.9);
        alteredDistributions2.addActionLabelledChoice(0, dist, "a'");
        dist = new Distribution();
        dist.add(1, 0.2);
        dist.add(2, 0.8);
        alteredDistributions2.addActionLabelledChoice(0, dist, "b'");
        dist = new Distribution();
        dist.add(1, 1.0);
        alteredDistributions2.addActionLabelledChoice(1, dist, "a'");
        dist = new Distribution();
        dist.add(2, 0.4);
        dist.add(3, 0.6);
        alteredDistributions2.addActionLabelledChoice(1, dist, "b'");
        dist = new Distribution();
        dist.add(0, 0.25);
        dist.add(1, 0.25);
        dist.add(2, 0.25);
        dist.add(3, 0.25);
        alteredDistributions2.addActionLabelledChoice(2, dist, "d");

        assertEquals(exportDotFileToString(alteredDistributions2), exportDotFileToString(alteredDistributions));
    }

    @Test
    public void additionalStates() throws PrismException {
        init();

        MDPAdditionalStates additionalStates = new MDPAdditionalStates(original1, 2);

        MDPSimple additionalStates2 = new MDPSimple(5);
        additionalStates2.addInitialState(0);
        Distribution<Double> dist = new Distribution();
        dist.add(1, 0.1);
        dist.add(2, 0.9);
        additionalStates2.addActionLabelledChoice(0, dist, "a");
        dist = new Distribution();
        dist.add(1, 0.2);
        dist.add(2, 0.8);
        additionalStates2.addActionLabelledChoice(0, dist, "b");
        dist = new Distribution();
        dist.add(1, 1.0);
        additionalStates2.addActionLabelledChoice(1, dist, "a");
        dist = new Distribution();
        dist.add(2, 1.0);
        additionalStates2.addActionLabelledChoice(1, dist, "b");

        assertEquals(exportDotFileToString(additionalStates2), exportDotFileToString(additionalStates));
        additionalStates.findDeadlocks(true);

        dist = new Distribution();
        dist.add(2, 1.0);
        additionalStates2.addChoice(2, dist);
        dist = new Distribution();
        dist.add(3, 1.0);
        additionalStates2.addChoice(3, dist);
        dist = new Distribution();
        dist.add(4, 1.0);
        additionalStates2.addChoice(4, dist);

        assertEquals(exportDotFileToString(additionalStates2), exportDotFileToString(additionalStates));


        // TODO Lupa
        additionalStates = new MDPAdditionalStates(original1, 2, true);

        additionalStates.findDeadlocks(true);
        assertEquals(exportDotFileToString(additionalStates2), exportDotFileToString(additionalStates));
    }


    @Test
    public void restricted() throws PrismException {
        init();

        final BitSet include = BitSetTools.asBitSet(1, 2);

        MDPRestricted restricted = new MDPRestricted(original1, include, Restriction.STRICT);
        restricted.findDeadlocks(true);

        MDPSimple restricted2 = new MDPSimple(2);
        restricted2.addInitialState(0);
        Distribution<Double> dist = new Distribution();
        dist.add(0, 1.0);
        restricted2.addActionLabelledChoice(0, dist, "a");
        dist = new Distribution();
        dist.add(1, 1.0);
        restricted2.addActionLabelledChoice(0, dist, "b");
        dist = new Distribution();
        dist.add(1, 1.0);
        restricted2.addChoice(1, dist);


        assertEquals(exportDotFileToString(restricted2), exportDotFileToString(restricted));

        restricted = new MDPRestricted(original1, include, Restriction.TRANSITIVE_CLOSURE);
        restricted.findDeadlocks(true);

        assertEquals(exportDotFileToString(restricted2), exportDotFileToString(restricted));

        restricted = new MDPRestricted(original1, include, Restriction.TRANSITIVE_CLOSURE_SAFE);
        restricted.findDeadlocks(true);

        assertEquals(exportDotFileToString(restricted2), exportDotFileToString(restricted));
    }
    @Test
    public void equiv() throws PrismException {
        init();

        EquivalenceRelationInteger eq = new EquivalenceRelationInteger(new IterableArray.Of<>(BitSetTools.asBitSet(1, 2)));
        MDPEquiv equiv = new MDPEquiv(original1, eq);
        equiv.findDeadlocks(true);

        MDPSimple equiv2 = new MDPSimple(3);
        equiv2.addInitialState(0);
        Distribution<Double> dist = new Distribution();
        dist.add(1, 1.0);
        equiv2.addActionLabelledChoice(0, dist, "a");
        dist = new Distribution();
        dist.add(1, 1.0);
        equiv2.addActionLabelledChoice(0, dist, "b");
        dist = new Distribution();
        dist.add(1, 1.0);
        equiv2.addActionLabelledChoice(1, dist, "a");
        dist = new Distribution();
        dist.add(1, 1.0);
        equiv2.addActionLabelledChoice(1, dist, "b");
        dist = new Distribution();
        dist.add(2, 1.0);
        equiv2.addChoice(2, dist);

        assertEquals(exportDotFileToString(equiv2), exportDotFileToString(equiv));
    }

    @Test
    public void MDPfromDTMCTest() throws PrismException {
        MDPSimple mdp2 = new MDPSimple(3);
        mdp2.addInitialState(0);
        Distribution<Double> dist = new Distribution();
        dist.add(0, 0.7);
        dist.add(1, 0.3);
        mdp2.addChoice(0, dist);
        dist = new Distribution();
        dist.add(2, 0.5);
        dist.add(0, 0.5);
        mdp2.addChoice(1, dist);
        dist = new Distribution();
        dist.add(2, 1.0);
        mdp2.addChoice(2, dist);

        final DTMCSimple<Double> original = new DTMCSimple<>(3);
        original.addInitialState(0);
        original.setProbability(0, 0, 0.7);
        original.setProbability(0, 1, 0.3);
        original.setProbability(1, 2, 0.5);
        original.setProbability(1, 0, 0.5);
        original.setProbability(2, 2, 1.0);

        final MDP<Double> mdp = new MDPFromDTMC(original);
        mdp.findDeadlocks(true);

        assertEquals(exportDotFileToString(mdp2), exportDotFileToString(mdp));

    }

    @Test
    public void choicesToStates() throws PrismException
    {
        init();
        MDP choiceModel = ChoicesToStates.choicesToStates(original1);


        MDPSimple choiceModel2 = new MDPSimple(9);
        choiceModel2.addInitialState(0);
        Distribution<Double> dist = new Distribution();
        dist.add(3, 1.0);
        choiceModel2.addActionLabelledChoice(0, dist, "a");
        dist = new Distribution();
        dist.add(6, 1.0);
        choiceModel2.addActionLabelledChoice(0, dist, "b");
        dist = new Distribution();
        dist.add(4, 1.0);
        choiceModel2.addActionLabelledChoice(1, dist, "a");
        dist = new Distribution();
        dist.add(7, 1.0);
        choiceModel2.addActionLabelledChoice(1, dist, "b");

        dist = new Distribution();
        dist.add(1, 0.1);
        dist.add(2, 0.9);
        choiceModel2.addActionLabelledChoice(3, dist, "a");
        dist = new Distribution();
        dist.add(1, 1.0);
        choiceModel2.addActionLabelledChoice(4, dist, "a");

        dist = new Distribution();
        dist.add(1, 0.2);
        dist.add(2, 0.8);
        choiceModel2.addActionLabelledChoice(6, dist, "b");

        dist = new Distribution();
        dist.add(2, 1.0);
        choiceModel2.addActionLabelledChoice(7, dist, "b");


        assertEquals(exportDotFileToString(choiceModel2), exportDotFileToString(choiceModel));

        choiceModel.findDeadlocks(true);

        dist = new Distribution();
        dist.add(2, 1.0);
        choiceModel2.addChoice(2, dist);
        dist = new Distribution();
        dist.add(5, 1.0);
        choiceModel2.addChoice(5, dist);
        dist = new Distribution();
        dist.add(8, 1.0);
        choiceModel2.addChoice(8, dist);

        assertEquals(exportDotFileToString(choiceModel2), exportDotFileToString(choiceModel));
    }


}

