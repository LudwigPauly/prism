package explicit.modelviews;

import common.BitSetTools;
import common.iterable.EmptyIterator;
import common.iterable.IterableArray;
import explicit.*;
import org.junit.jupiter.api.Test;
import prism.PrismException;
import prism.PrismPrintStreamLog;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DTMCModelviewsTest {

    DTMCSimple <Double> original1;
    DTMCSimple <Double> original2;

    public void init() throws PrismException{
        original1 = new DTMCSimple(4);
        original1.addInitialState(0);
        original1.setProbability(0, 1, 0.1);
        original1.setProbability(0, 2, 0.9);
        original1.setProbability(1, 2, 0.2);
        original1.setProbability(1, 1, 0.8);
        original1.setProbability(2, 2, 1.0);

        original2 = new DTMCSimple(2);
        original2.addInitialState(0);
        original2.setProbability(0, 0, 0.1);
        original2.setProbability(0, 1, 0.9);
        original2.setProbability(1, 0, 1.0);
    }

    public String exportDotFileToString(Model model) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        model.exportToDotFile(new PrismPrintStreamLog(new PrintStream(outputStream)));
        byte[] buffer = outputStream.toByteArray();

        return new String(buffer);
    }

    @Test
    public void disjointUnion() throws PrismException {
        init();

        DTMC union = new DTMCDisjointUnion(original1, original2);
        union.findDeadlocks(true);

        DTMCSimple <Double> union2 = new DTMCSimple(6);
        union2.addInitialState(0);
        union2.setProbability(0, 1, 0.1);
        union2.setProbability(0, 2, 0.9);
        union2.setProbability(1, 2, 0.2);
        union2.setProbability(1, 1, 0.8);
        union2.setProbability(2, 2, 1.0);
        union2.setProbability(3, 3, 1.0);
        union2.setProbability(4, 4, 0.1);
        union2.setProbability(4, 5, 0.9);
        union2.setProbability(5, 4, 1.0);


        assertEquals(exportDotFileToString(union2), exportDotFileToString(union));
    }

    @Test
    public void disjointUnionEmptyModel() throws PrismException {
        init();

        original2 = new DTMCSimple(3);

        DTMC union = new DTMCDisjointUnion(original1, original2);
        union.findDeadlocks(true);

        DTMCSimple <Double> union2 = new DTMCSimple(7);
        union2.addInitialState(0);
        union2.setProbability(0, 1, 0.1);
        union2.setProbability(0, 2, 0.9);
        union2.setProbability(1, 2, 0.2);
        union2.setProbability(1, 1, 0.8);
        union2.setProbability(2, 2, 1.0);
        union2.setProbability(3, 3, 1.0);
        union2.setProbability(4, 4, 1.0);
        union2.setProbability(5, 5, 1.0);
        union2.setProbability(6, 6, 1.0);

        assertEquals(exportDotFileToString(union2), exportDotFileToString(union));
    }



    @Test
    public void alterdDistributions() throws PrismException {
        init();

        final IntFunction<Iterator<Map.Entry<Integer, Double>>> transitions = new IntFunction<Iterator<Map.Entry<Integer, Double>>>()
        {
            @Override
            public Iterator<Map.Entry<Integer, Double>> apply(final int state)
            {
                final Distribution distribution = new Distribution();
                if (state == 0) {
                    return EmptyIterator.of();
                }
                if (state == 1) {
                    distribution.set(1, 0.4);
                    distribution.set(3, 0.6);
                } else if (state == 3) {
                    distribution.set(2, 1.0);
                    distribution.set(3, 0.0);
                } else {
                    return null;
                }
                return distribution.iterator();
            }
        };

        DTMC alteredDistributions = new DTMCAlteredDistributions(original1, transitions);
        alteredDistributions.findDeadlocks(true);

        DTMCSimple <Double> alteredDistributions2 = new DTMCSimple(4);
        alteredDistributions2.addInitialState(0);
        alteredDistributions2.setProbability(0, 0, 1.0);
        alteredDistributions2.setProbability(1, 1, 0.4);
        alteredDistributions2.setProbability(1, 3, 0.6);
        alteredDistributions2.setProbability(2, 2, 1.0);
        alteredDistributions2.setProbability(3, 2, 1.0);


        assertEquals(exportDotFileToString(alteredDistributions2), exportDotFileToString(alteredDistributions));
    }

    @Test
    public void additionalStates() throws PrismException {
        init();

        DTMC additionalStates = new DTMCAdditionalStates(original1, 2);

        DTMCSimple <Double> additionalStates2 = new DTMCSimple(6);
        additionalStates2.addInitialState(0);
        additionalStates2.setProbability(0, 1, 0.1);
        additionalStates2.setProbability(0, 2, 0.9);
        additionalStates2.setProbability(1, 2, 0.2);
        additionalStates2.setProbability(1, 1, 0.8);
        additionalStates2.setProbability(2, 2, 1.0);



        assertEquals(exportDotFileToString(additionalStates2), exportDotFileToString(additionalStates)); //TODO Lupa
        additionalStates.findDeadlocks(true);

        additionalStates2.setProbability(3, 3, 1.0);
        additionalStates2.setProbability(4, 4, 1.0);
        additionalStates2.setProbability(5, 5, 1.0);

        assertEquals(exportDotFileToString(additionalStates2), exportDotFileToString(additionalStates));


        // TODO  Lupa
        additionalStates = new DTMCAdditionalStates(original1, 2, true);

        additionalStates.findDeadlocks(true);
        assertEquals(exportDotFileToString(additionalStates2), exportDotFileToString(additionalStates));
    }


    @Test
    public void restricted() throws PrismException {
        init();

        final BitSet include = BitSetTools.asBitSet(1, 2);

        DTMC restricted = new DTMCRestricted(original1, include, Restriction.STRICT);
        restricted.findDeadlocks(true);

        DTMCSimple <Double> restricted2 = new DTMCSimple(2);
        restricted2.addInitialState(0);
        restricted2.setProbability(0, 1, 0.2);
        restricted2.setProbability(0, 0, 0.8);
        restricted2.setProbability(1, 1, 1.0);

        assertEquals(exportDotFileToString(restricted2), exportDotFileToString(restricted));

        restricted = new DTMCRestricted(original1, include, Restriction.TRANSITIVE_CLOSURE);
        restricted.findDeadlocks(true);

        assertEquals(exportDotFileToString(restricted2), exportDotFileToString(restricted));

        restricted = new DTMCRestricted(original1, include, Restriction.TRANSITIVE_CLOSURE_SAFE);
        restricted.findDeadlocks(true);

        assertEquals(exportDotFileToString(restricted2), exportDotFileToString(restricted));
    }


    @Test
    public void equiv() throws PrismException {
        init();

        EquivalenceRelationInteger eq = new EquivalenceRelationInteger(new IterableArray.Of<>(BitSetTools.asBitSet(1, 0)));
        DTMC equiv = new DTMCEquiv(original1, eq, false);
        equiv.findDeadlocks(true);

        DTMCSimple <Double> equiv2 = new DTMCSimple(4);
        equiv2.addInitialState(0);
        equiv2.setProbability(0, 0, 0.9);
        equiv2.setProbability(0, 2, 1.1);
        equiv2.setProbability(1, 1, 1.0);
        equiv2.setProbability(2, 2, 1.0);
        equiv2.setProbability(3, 3, 1.0);


        assertEquals(exportDotFileToString(equiv2), exportDotFileToString(equiv));

        equiv = new DTMCEquiv(original1, eq, true);
        equiv.findDeadlocks(true);

        equiv2.setProbability(0, 0, 0.45);
        equiv2.setProbability(0, 2, 0.55);

        assertEquals(exportDotFileToString(equiv2), exportDotFileToString(equiv));

    }

    @Test
    public void DTMCfromMDPTest() throws PrismException {
        MDPSimple original = new MDPSimple(3);
        original.addInitialState(0);
        Distribution<Double> dist = new Distribution();
        dist.add(0, 0.7);
        dist.add(1, 0.3);
        original.addChoice(0, dist);
        dist = new Distribution();
        dist.add(2, 0.5);
        dist.add(0, 0.5);
        original.addChoice(1, dist);
        dist = new Distribution();
        dist.add(2, 1.0);
        original.addChoice(2, dist);

        final DTMCSimple<Double> dtmc2 = new DTMCSimple<>(3);
        dtmc2.addInitialState(0);
        dtmc2.setProbability(0, 0, 0.7);
        dtmc2.setProbability(0, 1, 0.3);
        dtmc2.setProbability(1, 2, 0.5);
        dtmc2.setProbability(1, 0, 0.5);
        dtmc2.setProbability(2, 2, 1.0);

        DTMC dtmc = new DTMCFromMDP(original);
        dtmc.findDeadlocks(true);

        assertEquals(exportDotFileToString(dtmc2), exportDotFileToString(dtmc));

    }





}
