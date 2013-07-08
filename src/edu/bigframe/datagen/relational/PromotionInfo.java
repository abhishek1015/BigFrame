package edu.bigframe.datagen.relational;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class PromotionInfo {

	private ArrayList<Integer> promotionSK;
	private ArrayList<Integer> dateBeginSK;
	private ArrayList<Integer> dateEndSK;
	private ArrayList<Integer> productSK;
	
	public PromotionInfo() {
		
	}
	
	public ArrayList<Integer> getPromotionSK() {
		return promotionSK;
	}
	
	public ArrayList<Integer> getDateBeginSK() {
		return dateBeginSK;
	}
	
	public ArrayList<Integer> getDateEndSK() {
		return dateEndSK;
	}
	
	public ArrayList<Integer> getProductSK() {
		return productSK;
	}
	
	public void setPromotionSK(ArrayList<Integer> promtSK) {
		promotionSK = promtSK;
	}
	
	public void setDateBeginSK(ArrayList<Integer> datebegSK) {
		dateBeginSK = datebegSK;
	}
	
	public void setDateEndSK(ArrayList<Integer> dateendSK) {
		dateEndSK = dateendSK;
	}
	
	public void setProductSK(ArrayList<Integer> prodSK) {
		productSK = prodSK;
	}
	
	// So these numbers are TPCDS specific
	public Date getDateBySK(int dateSK) {
		if(dateSK < 2415022 || dateSK > 2488070) {
			return null;
		}
		int baseSK = 2415022;
		int offset = dateSK - baseSK;
		
		Calendar c = Calendar.getInstance();
		c.set(1900,  Calendar.JANUARY, 2);
		
		c.add(Calendar.DATE, offset);
		
		return c.getTime();
		
	}
}