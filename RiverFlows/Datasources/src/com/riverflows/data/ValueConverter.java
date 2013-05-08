package com.riverflows.data;

import java.util.Map;

import com.riverflows.data.Variable.CommonVariable;


public abstract class ValueConverter {
	/**
	 * 
	 * @param fromUnit
	 * @return array of the units that this converter can convert fromUnit to, or an empty array if none. Never returns null.
	 */
	public abstract String[] convertsTo(String fromUnit);
	
	public abstract Double convert(String fromUnit, String toUnit, Double value) throws IllegalArgumentException;
	
	public static void convertIfNecessary(Map<CommonVariable, CommonVariable> conversionMap, Series s) {
		CelsiusFahrenheitConverter converter = new CelsiusFahrenheitConverter();
		
		Variable variable = s.getVariable();
		
		CommonVariable toVar = conversionMap.get(variable.getCommonVariable());
		
		if(toVar == null) {
			return;
		}
		
		for(Reading r:s.getReadings()) {
			r.setValue(converter.convert(variable.getUnit(), toVar.getUnit(), r.getValue()));
			if(r.getQualifiers() == null) {
				r.setQualifiers("converted:" + toVar.getUnit());
			} else {
				r.setQualifiers(r.getQualifiers() + " converted:" + toVar.getUnit());
			}
		}
		
		s.setVariable(new Variable(toVar, variable.getId(), variable.getMagicNullValue()));
	}
}
