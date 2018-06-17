package com.nitin.json.patch.vo;

import java.util.Arrays;
import java.util.List;

public enum PatchOperationType {
	add(Arrays.asList(new Member[] {Member.OP, Member.PATH, Member.VALUE})), 
	remove(Arrays.asList(new Member[] {Member.OP, Member.PATH})), 
	replace(Arrays.asList(new Member[] {Member.OP, Member.PATH, Member.VALUE})), 
	move(Arrays.asList(new Member[] {Member.OP, Member.PATH, Member.FROM})), 
	copy(Arrays.asList(new Member[] {Member.OP, Member.PATH, Member.FROM})), 
	test(Arrays.asList(new Member[] {Member.OP, Member.PATH, Member.VALUE}));
	
	private List<Member> requiredMembers;

	public List<Member> getMembers() {
		return requiredMembers;
	}

	PatchOperationType(List<Member> members) {
		this.requiredMembers = members;
	}
	
	public static boolean isValidOperationType(String op) {
		for (PatchOperationType opType : PatchOperationType.values()) {
			if (op != null && opType.toString().equals(op))
				return true;
		}
		return false;
	}
	
	public enum Member{
		OP("op"), PATH("path"), VALUE("value"), FROM("from");
		private String name;
		public String getName() {
			return name;
		}
		Member(String name) {
			this.name = name;
		}
	}
}
