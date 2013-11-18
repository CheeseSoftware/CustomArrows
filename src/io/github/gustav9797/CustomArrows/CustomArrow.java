package io.github.gustav9797.CustomArrows;

public class CustomArrow
{
	private String type;
	public ImpactEffect impactEffect;
	public int cost;
	
	public CustomArrow(String type, ImpactEffect effect, int cost)
	{
		this.type = type;
		this.impactEffect = effect;
		this.cost = cost;
	}
	
	public String getType()
	{
		return type;
	}
}
