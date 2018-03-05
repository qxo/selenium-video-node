package com.aimmac23.hub.proxy;

import java.util.Map;

import org.openqa.grid.internal.utils.DefaultCapabilityMatcher;

/**
 * extend DefaultCapabilityMatcher and support select video node
 * and make videoOn=false work(switch video off)
 * 
 */
public class VideoOnCapabilityMatcher extends DefaultCapabilityMatcher {

	public static final String KEY="videoOn";
	
	public static final String VIDEO_OFF_KEY ="_videoOff";

	public VideoOnCapabilityMatcher() {
		super();
		this.addToConsider(KEY);
	}

	/**
	 * if requestedCapability( videoOn=false ) , then remove videoOn and add _videoOff=true
	 */
	@Override
	public boolean matches(Map<String, Object> nodeCapability, Map<String, Object> requestedCapability) {
		Object o = requestedCapability.get(KEY);
		if( o != null && ( "false".equals(o) 
				|| ( o instanceof Boolean && !Boolean.TRUE.equals(o) ) ) ){
			requestedCapability.remove(KEY);
			requestedCapability.put(VIDEO_OFF_KEY, "true");
		}
		return super.matches(nodeCapability, requestedCapability);
	}
	
}
