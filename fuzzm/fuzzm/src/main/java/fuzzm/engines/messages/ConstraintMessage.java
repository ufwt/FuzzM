/* 
 * Copyright (C) 2017, Rockwell Collins
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the 3-clause BSD license.  See the LICENSE file for details.
 * 
 */
package fuzzm.engines.messages;

import fuzzm.engines.EngineName;
import fuzzm.lustre.BooleanCtx;
import fuzzm.util.RatSignal;

/**
 * The constraint message contains a constraint for the solver.
 * Generated by the Test Heuristic Engine.
 * Consumed by the Solver Engine.
 */
public class ConstraintMessage extends FeatureMessage {
	
	public BooleanCtx prop;
	public BooleanCtx hyp;
	public RatSignal optimizationTarget;
	public RatSignal generalizationTarget;
	
	public ConstraintMessage(EngineName source, FeatureID id, BooleanCtx hyp, BooleanCtx prop, RatSignal optimizationTarget, RatSignal generalizationTarget) {
		super(source,QueueName.ConstraintMessage,id);
		this.hyp = hyp;
		this.prop = prop;
		//assert(target.size() > 0);
		assert(optimizationTarget != null);
		assert(generalizationTarget != null);
		this.optimizationTarget = optimizationTarget;
		this.generalizationTarget = generalizationTarget;
	}
	
	@Override
	public void handleAccept(MessageHandler handler) {
		// TODO Auto-generated method stub
		handler.handleMessage(this);
	}
	
	@Override
	public String toString() {
		return "Message: [Constraint] " + sequence + ":" + id + " : " + prop.getExpr();
	}

}