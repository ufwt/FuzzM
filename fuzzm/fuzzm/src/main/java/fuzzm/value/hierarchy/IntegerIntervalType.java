/* 
 * Copyright (C) 2017, Rockwell Collins
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the 3-clause BSD license.  See the LICENSE file for details.
 * 
 */
package fuzzm.value.hierarchy;

import jkind.lustre.NamedType;
import jkind.lustre.Type;

abstract public class IntegerIntervalType extends NumericIntervalType<IntegerType> implements IntegerTypeInterface {

	@Override
	public EvaluatableType<IntegerType> int_divide(EvaluatableValue right) {
		IntegerTypeInterface rv = ((IntegerTypeInterface) right);
		return rv.int_divide2(this);
	}

	@Override
	public EvaluatableType<IntegerType> int_divide2(IntegerType left) {
		throw new IllegalArgumentException();
	}

	@Override
	public EvaluatableType<IntegerType> int_divide2(IntegerIntervalType left) {
		throw new IllegalArgumentException(); 
	}
	
	@Override
	public EvaluatableType<IntegerType> modulus(EvaluatableValue right) {
		IntegerTypeInterface rv = ((IntegerTypeInterface) right);
		return rv.modulus2(this);
	}

	@Override
	public EvaluatableType<IntegerType> modulus2(IntegerType left) {
		throw new IllegalArgumentException();
	}

	@Override
	public EvaluatableType<IntegerType> modulus2(IntegerIntervalType left) {
		throw new IllegalArgumentException();
	}
	
	@Override
	public final Type getType() {
		return NamedType.BOOL;
	}
	
}
