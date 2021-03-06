/* 
 * Copyright (C) 2017, Rockwell Collins
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the 3-clause BSD license.  See the LICENSE file for details.
 * 
 */
package fuzzm.value.bound;

import fuzzm.value.hierarchy.EvaluatableValue;

public interface ContainsEvaluatableValue {
	public EvaluatableValue getValue();
}
