package com.projects.tradingMachine.server;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import com.projects.tradingMachine.utility.ServiceLifeCycle;
import com.projects.tradingMachine.utility.TradingMachineMessageConsumer;
import com.projects.tradingMachine.utility.Utility;
import com.projects.tradingMachine.utility.Utility.DestinationType;
import com.projects.tradingMachine.utility.marketData.MarketData;

/**
 *  Receives market data from a given queue.
 * */
public class MarketDataManager implements MessageListener, ServiceLifeCycle {
	private final TradingMachineMessageConsumer marketDataConsumer;
	private final ConcurrentMap<String, MarketData> marketDataRepository;
	
	public MarketDataManager(final Properties properties) throws JMSException {
		marketDataRepository = new ConcurrentHashMap<>();
		marketDataConsumer =  new TradingMachineMessageConsumer(properties.getProperty("activeMQ.url"), properties.getProperty("activeMQ.marketDataTopic"), DestinationType.Topic, this, "MarketDataManager", null,  null);
	}
	
	public MarketData get(final String symbol) {
		return marketDataRepository.getOrDefault(symbol, Utility.buildRandomMarketDataItem(symbol));
	}
	
	@Override
	public void onMessage(final Message message) {
		try {
			@SuppressWarnings("unchecked")
			final ArrayList<MarketData> marketDataList = (ArrayList<MarketData>)((ObjectMessage)message).getObject();
			marketDataList.forEach(marketDataItem -> 
									marketDataRepository.merge(marketDataItem.getSymbol(), marketDataItem, (oldValue, newValue) -> marketDataItem));
		} catch (final JMSException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void start() throws Exception {
		marketDataConsumer.start();
	}

	@Override
	public void stop() throws Exception {
		marketDataConsumer.stop();
	}
}
