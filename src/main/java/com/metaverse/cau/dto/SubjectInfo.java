package com.metaverse.cau.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class SubjectInfo {
	private String subjectName;
	private int credit;
	private int seats;
	private int filledSeats;
	
	synchronized public boolean enroll() {
		if(seats - filledSeats > 0) {
			filledSeats++;
			
			return true;
		}
		
		return false;
	}
	public void resetSeats() {
		filledSeats =0;
	}
	public int getLeftSeats() {
		return seats - filledSeats;
	}
}
