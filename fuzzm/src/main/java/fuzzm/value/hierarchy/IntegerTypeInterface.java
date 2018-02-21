package jfuzz.value.hierarchy;

public interface IntegerTypeInterface {
	
	public EvaluatableType<IntegerType> int_divide(EvaluatableValue right);
	public EvaluatableType<IntegerType> int_divide2(IntegerType left);
	public EvaluatableType<IntegerType> int_divide2(IntegerIntervalType left);

	public EvaluatableType<IntegerType> modulus(EvaluatableValue right);
	public EvaluatableType<IntegerType> modulus2(IntegerType left);
	public EvaluatableType<IntegerType> modulus2(IntegerIntervalType left);

}
