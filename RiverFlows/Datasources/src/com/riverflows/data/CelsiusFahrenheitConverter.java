package com.riverflows.data;


public class CelsiusFahrenheitConverter extends ValueConverter {

	@Override
	public String[] convertsTo(String fromUnit) {
		
		if(fromUnit.equals("°C")) {
			return new String[] {"°F"};
		} else if(fromUnit.equals("°F")) {
			return new String[] {"°C"};
		}
		
		return new String[]{};
	}

	@Override
	public Double convert(String fromUnit, String toUnit, Double value) {
		if(fromUnit.equals("°C") && toUnit.equals("°F")) {
			if(value == null) {
				return null;
			}
			return value * 1.8d + 32.0d;
		} else if(fromUnit.equals("°F") && toUnit.equals("°C")) {
			if(value == null) {
				return null;
			}
			return (value - 32.0d) / 1.8d;
		}
		throw new IllegalArgumentException(getClass().getName() + " cannot convert from " + fromUnit + " to " + toUnit);
	}
}
