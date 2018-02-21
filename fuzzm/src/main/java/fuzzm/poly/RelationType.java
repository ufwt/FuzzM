package jfuzz.poly;

public enum RelationType {
	INCLUSIVE, EXCLUSIVE;
	public RelationType inclusiveAND(RelationType arg) {
		return ((this == INCLUSIVE) && (arg == INCLUSIVE)) ? INCLUSIVE : EXCLUSIVE;
	}
	public RelationType inclusiveIFF(RelationType arg) {
		return (this == arg) ? INCLUSIVE : EXCLUSIVE;
	}
	public RelationType not() {
		return (this == INCLUSIVE) ? EXCLUSIVE : INCLUSIVE;
	}
	public boolean inclusiveEXCLUSIVE(RelationType arg) {
		return (this == INCLUSIVE) && (arg == EXCLUSIVE);
	}
	public int compareWith(RelationType arg) {
		// Compare the state space bounded by 'this' with that bounded by 'arg' .. which is bigger?
		if (this == arg) return 0;
		if (this == INCLUSIVE) return 1;
		return -1;
	}
}
