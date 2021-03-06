package it.unibo.arces.wot.sepa.tools;

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

public class PopulateExperiment extends SmartLightingBenchmark {
	public PopulateExperiment() throws FileNotFoundException, NoSuchElementException, IOException, UnrecoverableKeyException, KeyManagementException, IllegalArgumentException, KeyStoreException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, NullPointerException, ClassCastException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, URISyntaxException {
		super();
	}

	protected String tag ="LampExp";
	protected static SmartLightingBenchmark benchmark = null;
	
	//Data set
	protected int roads[] = {100,100,100,10};
	protected int roadSizes[] = {10,25,50,100};
	
	//Road subscriptions
	protected int roadSubscriptionRoads[] = {};
	
	//Lamp subscriptions
	protected int lampSubscriptionRoads[][] ={};
	protected int lampSubscriptionLamps[][] ={};
	
	@Override
	public void reset() {
			
	}

	@Override
	public void runExperiment() {
				
	}

	@Override
	public void dataset() {
		//Data set creation
		int roadIndex = firstRoadIndex;	
		nRoads = 0;
		for (int i=0; i < roads.length; i++) {
			roadIndex = addRoads(roads[i],roadSizes[i],roadIndex);	
			nRoads = nRoads + roads[i];
		}
	}

	@Override
	public void subscribe() {
		
		
	}
	
	public static void main(String[] args)  {
		try {
			benchmark = new PopulateExperiment();
		} catch (UnrecoverableKeyException | KeyManagementException | NoSuchElementException | IllegalArgumentException
				| KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | InvalidKeyException | NullPointerException | ClassCastException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | URISyntaxException e) {
			e.printStackTrace();
			System.exit(1);
		}
		try {
			benchmark.run(true,true,5000);
		} catch (UnrecoverableKeyException | KeyManagementException | IllegalArgumentException | KeyStoreException
				| NoSuchAlgorithmException | CertificateException | IOException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | URISyntaxException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
