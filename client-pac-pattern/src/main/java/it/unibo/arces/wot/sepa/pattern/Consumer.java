/* This class abstracts a consumer of the SEPA Application Design Pattern
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.pattern;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.NoSuchElementException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;

import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;

public abstract class Consumer extends Client implements IConsumer {
	protected String sparqlSubscribe = null;
	protected String subID = "";
	protected boolean onSubscribe = true;
	protected int DEFAULT_SUBSCRIPTION_TIMEOUT = 3000;
	protected SubcribeConfirmSync subConfirm;

	protected DefaultNotificationHandler handler = new DefaultNotificationHandler() {
		@Override
		public void onSemanticEvent(Notification notify) {
			ARBindingsResults results = notify.getARBindingsResults();

			BindingsResults added = results.getAddedBindings();
			BindingsResults removed = results.getRemovedBindings();

			// Replace prefixes
			for (Bindings bindings : added.getBindings()) {
				for (String var : bindings.getVariables()) {
					if (bindings.isURI(var)) {
						for (String prefix : URI2PrefixMap.keySet())
							if (bindings.getBindingValue(var).startsWith(prefix)) {
								bindings.addBinding(var, new RDFTermURI(bindings.getBindingValue(var).replace(prefix,
										URI2PrefixMap.get(prefix) + ":")));
								break;
							}
					}
				}
			}
			for (Bindings bindings : removed.getBindings()) {
				for (String var : bindings.getVariables()) {
					if (bindings.isURI(var)) {
						for (String prefix : URI2PrefixMap.keySet())
							if (bindings.getBindingValue(var).startsWith(prefix)) {
								bindings.addBinding(var, new RDFTermURI(bindings.getBindingValue(var).replace(prefix,
										URI2PrefixMap.get(prefix) + ":")));
								break;
							}
					}
				}
			}

			if (onSubscribe) {
				onSubscribe = false;
				onSubscribe(added);
				return;
			}

			// Dispatch different notifications based on notify content
			if (!added.isEmpty())
				onAddedResults(added);
			if (!removed.isEmpty())
				onRemovedResults(removed);
			onResults(results);

		}

		@Override
		public void onSubscribeConfirm(SubscribeResponse response) {
			logger.debug("Subscribe confirmed " + response.getSpuid() + " alias: " + response.getAlias());
			subConfirm.notifySubscribeConfirm(response.getSpuid());
		}

		@Override
		public void onUnsubscribeConfirm(UnsubscribeResponse response) {
			logger.debug("Unsubscribe confirmed " + response.getSpuid());
			onUnsubscribe();
		}
	};

	private static final Logger logger = LogManager.getLogger("Consumer");

	protected class SubcribeConfirmSync {
		private String subID = "";

		public synchronized String waitSubscribeConfirm(int timeout) {

			if (!subID.equals(""))
				return subID;

			try {
				logger.debug("Wait for subscribe confirm...");
				wait(timeout);
			} catch (InterruptedException e) {

			}

			return subID;
		}

		public synchronized void notifySubscribeConfirm(String spuid) {
			logger.debug("Notify confirm!");

			subID = spuid;
			notifyAll();
		}
	}

	public Consumer(ApplicationProfile appProfile, String subscribeID)
			throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
		super(appProfile);

		if (appProfile == null || subscribeID == null) {
			logger.fatal("One or more arguments are null");
			throw new IllegalArgumentException("One or more arguments are null");
		}

		if (appProfile.subscribe(subscribeID) == null) {
			logger.fatal("SUBSCRIBE ID " + subscribeID + " not found in " + appProfile.getFileName());
			throw new IllegalArgumentException(
					"SUBSCRIBE ID " + subscribeID + " not found in " + appProfile.getFileName());
		}

		sparqlSubscribe = appProfile.subscribe(subscribeID);
		
		protocolClient.setNotificationHandler(handler);
	}

	public Consumer(String jparFile) throws IllegalArgumentException, FileNotFoundException, NoSuchElementException,
			IOException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException,
			CertificateException, InvalidKeyException, NullPointerException, ClassCastException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, URISyntaxException {
		super(jparFile);
		
		protocolClient.setNotificationHandler(handler);
	}

	public String subscribe(Bindings forcedBindings)
			throws IOException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException,
			UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		if (sparqlSubscribe == null) {
			logger.fatal("SPARQL SUBSCRIBE not defined");
			return null;
		}

		if (protocolClient == null) {
			logger.fatal("Client not initialized");
			return null;
		}

		String sparql = prefixes() + replaceBindings(sparqlSubscribe, forcedBindings);

		logger.debug("<SUBSCRIBE> ==> " + sparql);

		onSubscribe = true;

		subConfirm = new SubcribeConfirmSync();

		Response response = protocolClient.subscribe(new SubscribeRequest(sparql));

		logger.debug(response.toString());

		if (response.getClass().equals(ErrorResponse.class))
			return null;

		subID = subConfirm.waitSubscribeConfirm(DEFAULT_SUBSCRIPTION_TIMEOUT);

		return subID;

	}

	public boolean unsubscribe() throws IOException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException,
			UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		logger.debug("UNSUBSCRIBE " + subID);

		if (protocolClient == null) {
			logger.fatal("Client not initialized");
			return false;
		}

		Response response;

		response = protocolClient.unsubscribe(new UnsubscribeRequest(subID));

		logger.debug(response.toString());

		return !(response.getClass().equals(ErrorResponse.class));
	}
}
