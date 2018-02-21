package jfuzz.poly;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import jfuzz.lustre.SignalName;
import jfuzz.lustre.evaluation.PolyFunctionMap;
import jfuzz.solver.SolverResults;
import jfuzz.util.Debug;
import jfuzz.util.ID;
import jfuzz.util.IntervalVector;
import jfuzz.util.JFuzzInterval;
import jfuzz.util.ProofWriter;
import jfuzz.util.Rat;
import jfuzz.util.RatSignal;
import jfuzz.util.RatVect;
import jfuzz.util.TypedName;
import jfuzz.value.instance.RationalValue;
import jfuzz.value.poly.GlobalState;
import jkind.lustre.NamedType;
import jkind.util.BigFraction;

public class VariableList extends LinkedList<Variable> {

	private static final long serialVersionUID = 7983100382771154597L;
	
	public VariableList() {
		super();
	}
	
	public VariableList(VariableList arg) {
		super(arg);
	}
	
	public VariableList(Variable c) {
		super();
		addLast(c);
	}
	
	public VariableList not() {
		VariableList res = new VariableList();
		for (Variable vc: this) {
			res.addLast(vc.not());
		}
		return res;
	}
	
	@Override
	public boolean add(Variable b) {
		throw new IllegalArgumentException();
	}
	
	public static void printAddFirst(String alt, Variable b, Variable first) {
		System.out.println("addFirst"+alt+"("+b.vid+":"+b.vid.level()+","+first.vid+":"+first.vid.level() + ",...)");
	}
	
	public static void printAddLast(String alt, Variable last, Variable b) {
		System.out.println("addLast"+alt+"(...,"+last.vid+":"+last.vid.level()+","+b.vid+":"+b.vid.level()+")");
	}
	
	@Override
	public void addFirst(Variable b) {
		// Maintain the ordering invariant ..
		if (! isEmpty()) {
			if (b.vid.compareTo(this.peekFirst().vid) >= 0) {
				printAddFirst("",b,this.peekFirst());				
				throw new IllegalArgumentException();
			}
		}
		super.addFirst(b);
	}
	
	@Override
	public void addLast(Variable b) {
		// Maintain the ordering invariant ..
		if (! isEmpty()) {
			if (this.peekLast().vid.compareTo(b.vid) >= 0) {
				printAddLast("",this.peekLast(),b);
				throw new IllegalArgumentException();
			}
		}
		super.addLast(b);
	}
	
	public void addFirstRev(Variable b) {
		// Maintain the ordering invariant ..
		if (! isEmpty()) {
			if (b.vid.compareTo(this.peekFirst().vid) <= 0) {
				printAddFirst("Rev",b,this.peekFirst());				
				throw new IllegalArgumentException();
			}
		}
		super.addFirst(b);
	}
	
	public void addLastRev(Variable b) {
		// Maintain the ordering invariant ..
		if (! isEmpty()) {
			if (this.peekLast().vid.compareTo(b.vid) <= 0) {
				printAddLast("Rev",this.peekLast(),b);
				throw new IllegalArgumentException();
			}
		}
		super.addLast(b);
	}
	
	static RestrictionResult andTrue(Variable x, Variable y) {
		RestrictionResult res = x.andTrue(y);
		if (Debug.isEnabled()) {
			String xacl2 = x.toACL2();
			String yacl2 = y.toACL2();
			String racl2 = res.toACL2();
			ProofWriter.printThms("andTrue",xacl2,yacl2, racl2); 
		}
		return res;
	}
	
	public static VariableList and(AndType andType, VariableList x, VariableList y) {
		VariableList ctx = new VariableList();
		if (x.isEmpty()) return y;
		if (y.isEmpty()) return x;
		Iterator<Variable> xit = x.iterator();
		Iterator<Variable> yit = y.iterator();
		Variable xv = xit.next();
		Variable yv = yit.next();
		//System.out.println("and() ..");
		while (true) {
			//System.out.println("xv:" + xv.vid + ":" + xv.vid.level());
			//System.out.println("yv:" + yv.vid + ":" + yv.vid.level());
			int cmp = xv.vid.compareTo(yv.vid);
			// Normal list is ordered from smallest to largest.
			if (cmp > 0) {
				// x is greater than y ..
				//if (Debug.isEnabled()) System.out.println(ID.location() + "(T and " + yv + ") = " + yv);
				ctx.addFirstRev(yv);
				if (! yit.hasNext()) {
					ctx.addFirstRev(xv);
					break;
				}
				yv = yit.next();
			} else if (cmp < 0){
				ctx.addFirstRev(xv);
				//if (Debug.isEnabled()) System.out.println(ID.location() + "(" + xv + " and T) = " + xv);
				if (! xit.hasNext()) {
					ctx.addFirstRev(yv);
					break;
				}
				xv = xit.next();
			} else {
				if (andType == AndType.TRUE) {
					RestrictionResult rr = andTrue(xv,yv);
					//if (Debug.isEnabled()) System.out.println(ID.location() + "(" + xv + " and " + yv + ") = " + rr.newConstraint);
					for (Variable r: rr.restrictionList) {
						ctx = restrict(r,ctx.iterator());
					}
					ctx.addFirstRev(rr.newConstraint);
				} else {
					//if (Debug.isEnabled()) System.out.println("Here : (" + xv + " and " + yv + ")");
					ctx.addFirstRev(xv.andFalse(yv));
				}
				if (! (xit.hasNext() && yit.hasNext())) break;
				xv = xit.next();
				yv = yit.next();
			}
		}
		while (xit.hasNext()) {
			ctx.addFirstRev(xit.next());
		}
		while (yit.hasNext()) {
			ctx.addFirstRev(yit.next());
		}
		Collections.reverse(ctx);
		//System.out.println("and().");
		return ctx;
	}
	
	public static VariableList restrict(Variable c, Iterator<Variable> xit) {
		//System.out.println("Push " + c.vid + ":" + c.vid.level() + " ..");
		//if (Debug.isEnabled()) System.out.println(ID.location() + "Restriction : " + c);
		VariableList res = new VariableList();
		while (xit.hasNext()) {
			// Reversed list is ordered from largest to smallest
			Variable xv = xit.next();
			//System.out.println("Restriction : " + c.vid+":"+c.vid.level());
			//System.out.println("Location    : " + xv.vid+":"+xv.vid.level());
			int cmp = xv.vid.compareTo(c.vid);
			if (cmp > 0) {
				res.addLastRev(xv);
			} else {
				if (cmp < 0) {
					res.addLastRev(c);
					c = xv;
				} else {
					RestrictionResult rr = andTrue(xv,c);
					//if (Debug.isEnabled()) System.out.println(ID.location() + "("+xv+" ^ "+c+") = " + rr.newConstraint);
					for (Variable vc: rr.restrictionList) {
						VariableList pres = restrict(vc,xit);
						xit = pres.iterator();
					}
					c = rr.newConstraint;
				}
				while (xit.hasNext()) {
					res.addLastRev(c);
					c = xit.next();
				}
			}
		}
		res.addLastRev(c);
		//System.out.println("Pop.");
		return res;
	}
	
	public VariableList applyRewrites() {
		Map<VariableID,AbstractPoly> rewrite = new HashMap<>();
		VariableList res = new VariableList();
		for (Variable v: this) {
			v = v.rewrite(rewrite);
			boolean keep = true;
			if (v instanceof VariableEquality) {				
				VariableEquality veq = (VariableEquality) v;
				if (veq.relation == RelationType.INCLUSIVE) {
					rewrite.put(veq.vid, veq.poly);
					keep = (veq.vid.type != VariableType.AUXILIARY);
				}
			}
			if (keep) res.addLast(v);
		}
		return res;
	}
	
	public VariableList normalize() {
		VariableList x = this.applyRewrites();
		boolean changed;
		do {
			changed = false;
			x = new VariableList(x);
			Collections.reverse(x);
			VariableList res = new VariableList();
			while (! x.isEmpty()) {
				//System.out.println(ID.location() + "Generalization size : " + x.size());
				//System.out.println(ID.location() + x);
				Variable v = x.poll();
				if (v.implicitEquality()) {
					//System.out.println(ID.location() + "Implicit Equality : " + v);
					v = v.toEquality();
					changed = true;
				}
				if (v.slackIntegerBounds()) {
					//System.out.println(ID.location() + "Slack Bounds : " + v);
					v = v.tightenIntegerBounds();
					changed = true;
				}
				if (v.reducableIntegerInterval()) {
					//System.out.println(ID.location() + "reducableIntegerInterval : " + v);
					RestrictionResult er = v.reducedInterval();
					//System.out.println(ID.location() + "reducedInterval : " + er);
					v = er.newConstraint;
					for (Variable r: er.restrictionList) {
						x = restrict(r,x.iterator());
					}
					changed = true;
				}
				if (v.requiresRestriction()) {
					RestrictionResult rr = v.restriction();
					v = rr.newConstraint;
					for (Variable r: rr.restrictionList) {
						x = restrict(r,x.iterator());
					}
				}
				res.addFirst(v);
			}
			x = res.applyRewrites();
		} while (changed);
		return x;
	}
	
	public VariableList chooseAndNegateOne() {
		Variable one = null;
		int max = 0;
		for (Variable v : this) {
			if (v.countFeatures() >= max) {
				one = v;
				max = v.countFeatures();
			}
		}
		assert(one != null);
		return new VariableList(one.not());
	}

	public RatSignal randomVector(boolean biased, BigFraction Smin, BigFraction Smax, IntervalVector span, Map<VariableID,BigFraction> ctx) {
		//int tries = 100;
	    @SuppressWarnings("unused")
        int bools = 0;
	    while (true) {
			try {
				RatSignal res = new RatSignal();
				for (Variable c: this) {
					RegionBounds r;
					try {
						r = c.constraintBounds(ctx);
					} catch (EmptyIntervalException e) {
						System.out.println(ID.location() + "Constraint : " + c);
						System.out.println(ID.location() + "Context : " + ctx);
						throw new IllegalArgumentException();
					}
					VariableID vid = c.vid;
					SignalName sn  = vid.name;
					TypedName  name = sn.name;
					NamedType type = c.vid.name.name.type;
					JFuzzInterval bounds;
					try {
						bounds = span.containsKey(name) ? r.fuzzInterval(span.get(name)) : r.fuzzInterval(type, Smin, Smax);			
					} catch (EmptyIntervalException e) {
						System.out.println(ID.location() + "Constraint : " + c);
						System.out.println(ID.location() + "Context : " + ctx);
						System.out.println(ID.location() + "RegionBounds : " + r);
						throw new IllegalArgumentException();
					}
					BigFraction value;
					if (r.rangeType == RelationType.INCLUSIVE) {				
						try {
							value = Rat.biasedRandom(type, biased, 0, bounds.min, bounds.max);		
						} catch (EmptyIntervalException e) {
							System.out.println(ID.location() + "Constraint : " + c);
							System.out.println(ID.location() + "CEX String : " + c.cexString());
							System.out.println(ID.location() + "Context : " + ctx);
							System.out.println(ID.location() + "RegionBounds : " + r);
							System.out.println(ID.location() + "JFuzzBounds  : " + bounds);
							throw new IllegalArgumentException();
						}
					} else {
						BigFraction upper = ((RationalValue) r.upper).value();
						BigFraction lower = ((RationalValue) r.lower).value();
						BigFraction one   = type == NamedType.INT ? BigFraction.ONE : BigFraction.ZERO;
						BigFraction max = bounds.max.add(lower.subtract(bounds.min)).add(one);
						value = Rat.biasedRandom(type, biased, 0, upper, max);
					}
					if (sn.time >= 0) res.put(sn.time, sn.name, value);
					ctx.put(vid, value);
					if (! c.evalCEX(ctx)) {
						System.out.println(ID.location() + "Constraint : " + c);
						System.out.println(ID.location() + "Context : " + ctx);
						System.out.println(ID.location() + "RegionBounds : " + r);
						System.out.println(ID.location() + "JFuzzBounds  : " + bounds);
						assert(false);
					}
				}
				// We should probably do this for all the variable types ..
				for (TypedName z : span.keySet()) {
					if (z.type == NamedType.BOOL) {
						for (RatVect rv: res) {
							if (! rv.containsKey(z)) {
								rv.put(z, GlobalState.oracle().nextBoolean() ? BigFraction.ONE : BigFraction.ZERO);
							}
						}
					}
				}
				return res;
			} catch (EmptyIntervalException e) {
				throw new IllegalArgumentException(e);
//				tries--;
//				if (tries <= 0) throw e;
//				continue;
			}
		}
	}

	public Collection<VariableID> unboundVariables() {
		Set<VariableID> bound = new HashSet<>();		
		Set<VariableID> used  = new HashSet<>();
		for (Variable v: this) {
			bound.add(v.vid);
		}
		for (Variable v: this) {
			used = v.updateVariableSet(used);
		}
		used.removeAll(bound);
		return used;
	}
	
	public Map<VariableID,RegionBounds> intervalBounds() {
		Map<VariableID,RegionBounds> res = new HashMap<>();
		for (Variable v: this) {
			RegionBounds b = v.intervalBounds(res);
			res.put(v.vid, b);
		}
		return res;
	}

	public SolverResults optimize(SolverResults sln, PolyFunctionMap fmap, RatSignal target) {
		RatSignal res = new RatSignal(sln.cex);
		RatVect tempVars = new RatVect();
		VariableList z = new VariableList(this);
		// We need to do this top to bottom.
        Collections.reverse(z);
		while (! z.isEmpty()) {
		    Variable v = z.poll();
			RegionBounds interval;
			BigFraction value;            
			if (v instanceof VariableBoolean) {
			    value = v.vid.cex;
			    int time = v.vid.name.time;
                TypedName tname = v.vid.name.name;
                if (time >= 0) {
                    res.put(v.vid.name.time,v.vid.name.name,value);
                } else {
                    tempVars.put(tname, value);
                }
			} else {
			    try {
			        interval = v.constraintBounds().fix(v.vid.name.name.type);
			    } catch (EmptyIntervalException e) {
			        System.out.println(ID.location() + "Interval Bound Violation on " + v);
			        throw e;
			    }
			    int time = v.vid.name.time;
			    TypedName tname = v.vid.name.name;
			    NamedType type = v.vid.name.name.type;
			    if (time >= 0) {
			        value = interval.optimize(type,target.get(time).get(tname));
			        res.put(v.vid.name.time,v.vid.name.name,value);
			    } else {
			        value = interval.optimize(type, v.vid.getCex());
			        tempVars.put(tname, value);
			    }
			    // Now restrict the remaining constraints so that 
			    // they always at least contain "value"
			    RestrictionResult rr = v.mustContain(new PolyBase(value));
                v = rr.newConstraint;
                for (Variable r: rr.restrictionList) {
                    z = restrict(r,z.iterator());
                }
			}
			// assert(v.evalCEX(ctx)) : "Failure to preserve " + v + " with " + value + " under " + ctx;
		}
		fmap.updateFunctions(tempVars, sln.fns);
		return new SolverResults(res,sln.fns);		
	}
	
	public int[] gradiantToDirectionMatrix(AbstractPoly gradiant) {
		int direction[] = new int[this.size()];
		Iterator<Variable> it = this.descendingIterator();
		int index = this.size();
		while (it.hasNext()) {
			index--;
			Variable v = it.next();
			int sign = gradiant.getCoefficient(v.vid).signum();
			direction[index] = sign;
			AbstractPoly bound = v.maxBound(sign);
			gradiant = gradiant.remove(v.vid);
			BigFraction N = gradiant.dot(bound);
			BigFraction D = bound.dot(bound);
			gradiant = gradiant.subtract(bound.divide(N.divide(D)));
			if (gradiant.isConstant()) break;
		}
		while (it.hasNext()) {
			index--;
			it.next();
			direction[index] = 1;
		}
		assert(index == 0);
		return direction;
	}
	
	public Map<VariableID,BigFraction> maximize(int direction[]) {
		int index = 0;
		Map<VariableID,BigFraction> ctx = new HashMap<>();
		for (Variable v: this) {
			BigFraction value = v.maxValue(direction[index], ctx);
			ctx.put(v.vid, value);
			index++;
		}
		return ctx;
	}

}
