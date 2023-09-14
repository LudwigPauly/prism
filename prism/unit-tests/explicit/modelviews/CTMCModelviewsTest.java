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
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CTMCModelviewsTest {
    CTMCSimple<Double> original1;
    CTMCSimple <Double> original2;

    public void init() throws PrismException {
        original1 = new CTMCSimple(4);
        original1.addInitialState(0);
        original1.setProbability(0, 1, 0.1);
        original1.setProbability(0, 2, 0.9);
        original1.setProbability(1, 2, 0.2);
        original1.setProbability(1, 1, 0.8);
        original1.setProbability(2, 2, 1.0);

        original2 = new CTMCSimple(2);
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

        CTMC union = new CTMCDisjointUnion(original1, original2);
        union.findDeadlocks(true);

        CTMCSimple <Double> union2 = new CTMCSimple(6);
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

        original2 = new CTMCSimple(3);

        CTMC union = new CTMCDisjointUnion(original1, original2);
        union.findDeadlocks(true);

        CTMCSimple <Double> union2 = new CTMCSimple(7);
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

        CTMC alteredDistributions = new CTMCAlteredDistributions(original1, transitions);
        alteredDistributions.findDeadlocks(true);

        CTMCSimple <Double> alteredDistributions2 = new CTMCSimple(4);
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

        CTMC additionalStates = new CTMCAdditionalStates(original1, 2);

        CTMCSimple <Double> additionalStates2 = new CTMCSimple(6);
        additionalStates2.addInitialState(0);
        additionalStates2.setProbability(0, 1, 0.1);
        additionalStates2.setProbability(0, 2, 0.9);
        additionalStates2.setProbability(1, 2, 0.2);
        additionalStates2.setProbability(1, 1, 0.8);
        additionalStates2.setProbability(2, 2, 1.0);



        assertEquals(exportDotFileToString(additionalStates2), exportDotFileToString(additionalStates));
        additionalStates.findDeadlocks(true);

        additionalStates2.setProbability(3, 3, 1.0);
        additionalStates2.setProbability(4, 4, 1.0);
        additionalStates2.setProbability(5, 5, 1.0);

        assertEquals(exportDotFileToString(additionalStates2), exportDotFileToString(additionalStates));

        additionalStates = new CTMCAdditionalStates(original1, 2, true);

        additionalStates.findDeadlocks(true);
        assertEquals(exportDotFileToString(additionalStates2), exportDotFileToString(additionalStates));
    }


    @Test
    public void restricted() throws PrismException {
        init();

        final BitSet include = BitSetTools.asBitSet(1, 2);

        CTMC restricted = new CTMCRestricted(original1, include, Restriction.STRICT);
        restricted.findDeadlocks(true);

        CTMCSimple <Double> restricted2 = new CTMCSimple(2);
        restricted2.addInitialState(0);
        restricted2.setProbability(0, 1, 0.2);
        restricted2.setProbability(0, 0, 0.8);
        restricted2.setProbability(1, 1, 1.0);

        assertEquals(exportDotFileToString(restricted2), exportDotFileToString(restricted));

        restricted = new CTMCRestricted(original1, include, Restriction.TRANSITIVE_CLOSURE);
        restricted.findDeadlocks(true);

        assertEquals(exportDotFileToString(restricted2), exportDotFileToString(restricted));

        restricted = new CTMCRestricted(original1, include, Restriction.TRANSITIVE_CLOSURE_SAFE);
        restricted.findDeadlocks(true);

        assertEquals(exportDotFileToString(restricted2), exportDotFileToString(restricted));
    }


    @Test
    public void equiv() throws PrismException {
        init();

        EquivalenceRelationInteger eq = new EquivalenceRelationInteger(new IterableArray.Of<>(BitSetTools.asBitSet(1, 0)));
        CTMC equiv = new CTMCEquiv(original1, eq, false);
        equiv.findDeadlocks(true);

        CTMCSimple <Double> equiv2 = new CTMCSimple(4);
        equiv2.addInitialState(0);
        equiv2.setProbability(0, 0, 0.9);
        equiv2.setProbability(0, 2, 1.1);
        equiv2.setProbability(1, 1, 1.0);
        equiv2.setProbability(2, 2, 1.0);
        equiv2.setProbability(3, 3, 1.0);


        assertEquals(exportDotFileToString(equiv2), exportDotFileToString(equiv));

        equiv = new CTMCEquiv(original1, eq, true);
        equiv.findDeadlocks(true);

        equiv2.setProbability(0, 0, 0.45);
        equiv2.setProbability(0, 2, 0.55);

        assertEquals(exportDotFileToString(equiv2), exportDotFileToString(equiv));

    }
    
}
