// PublicObservable.java
// PublicObservable
// 
// Author: Michael Morris

package com.bestos.thebestcrawler.workers;

import java.util.Observable;

public class PublicObservable extends Observable {
	
	@Override
	public void setChanged() {
		this.setChanged();
	}
	
}
