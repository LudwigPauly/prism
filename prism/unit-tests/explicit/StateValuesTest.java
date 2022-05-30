package explicit;


import org.junit.jupiter.api.Test;
import parser.type.TypeDouble;
import prism.PrismException;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class StateValuesTest {

	//Type retType, StateValues.BinaryFunction func, StateValues sv2, BitSet filter

	StateValues stateValues1;
	StateValues stateValues2;
	BitSet bitSet;

	public void init() throws PrismException
	{
		DTMC dtmc = new DTMCSimple(3);


		double [] doubles = new double[3];
		doubles[0] = 5;
		doubles[1] = 6;
		doubles[2] = 9;

		stateValues1 = StateValues.createFromDoubleArray(doubles,dtmc);
		stateValues2 = StateValues.createFromDoubleArray(doubles,dtmc);


		bitSet = new BitSet(3);
		bitSet.set(1,true);
	}

	@Test
	public void testApplyFunction() throws PrismException
	{
		init();

		stateValues1.applyFunction(TypeDouble.getInstance(), (v1,v2) -> (double)v1 * (double)v2,stateValues2,bitSet);

		stateValues2.setValue(1,36.0);

		assertArrayEquals(stateValues2.getDoubleArray(),stateValues1.getDoubleArray());
	}

	@Test
	public void testSumOverBitSet() throws PrismException
	{
		init();
		bitSet.set(2,true);

		double d = (double)stateValues1.sumOverBitSet(bitSet);
		assertEquals(15.0,d);

	}


}