package openeye.logic;

class CrashId {
	public long timestamp;
	public int id;

	CrashId(long timestamp, int id) {
		this.timestamp = timestamp;
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + (int)(timestamp ^ (timestamp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;

		if (obj instanceof CrashId) {
			CrashId other = (CrashId)obj;
			return id == other.id && timestamp == other.timestamp;
		}
		return false;
	}

}