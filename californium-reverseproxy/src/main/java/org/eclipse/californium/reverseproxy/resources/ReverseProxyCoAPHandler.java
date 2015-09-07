package org.eclipse.californium.reverseproxy.resources;
import java.util.Date;
import java.util.List;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.reverseproxy.PeriodicRequest;

/**
 * Response Handler for notifications coming from the end device.
 */
public class ReverseProxyCoAPHandler implements CoapHandler{

	private ReverseProxyResource ownerResource;
	public ReverseProxyCoAPHandler(ReverseProxyResource ownerResource){
		this.ownerResource = ownerResource;
	}
	
	@Override
	public void onLoad(CoapResponse coapResponse) {
		Response response = coapResponse.advanced();
		ownerResource.getNotificationOrderer().getNextObserveNumber();
		if(ownerResource.getLastNotificationMessage() == null){
			List<PeriodicRequest> tmp = ownerResource.getSubscriberList();
			for(PeriodicRequest pr : tmp){
				if(pr.isAllowed()){
					pr.setLastNotificationSent(response);
					Date now = new Date();
					long timestamp = now.getTime();
					pr.setTimestampLastNotificationSent(timestamp);
					Response responseForClients = new Response(response.getCode());
					// copy payload
					byte[] payload = response.getPayload();
					responseForClients.setPayload(payload);
		
					// copy the timestamp
					
					responseForClients.setTimestamp(timestamp);
		
					// copy every option
					responseForClients.setOptions(new OptionSet(response.getOptions()));
					responseForClients.getOptions().setMaxAge(pr.getPmax() / 1000);		
					responseForClients.setDestination(pr.getClientEndpoint().getRemoteAddress());
					responseForClients.setDestinationPort(pr.getClientEndpoint().getRemotePort());
					responseForClients.setToken(pr.getToken());
					pr.getExchange().respond(responseForClients);
					
				}
			}
		}
		
		ownerResource.updateRTT(response.getRemoteEndpoint().getCurrentRTO());
		Date now = new Date();
		long timestamp = now.getTime();
		response.setTimestamp(timestamp);
		ownerResource.setLastNotificationMessage(response);
		ownerResource.lock.lock();
		ownerResource.newNotification.signalAll();
		ownerResource.lock.unlock();
	}

	@Override
	public void onError() {
		// TODO Auto-generated method stub
		
	}

}