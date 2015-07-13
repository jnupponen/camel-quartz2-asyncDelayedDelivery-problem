package com.example.route;

import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.WARN;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.joda.time.DateTime;

public class MyAsyncDelayedDeliveryTesting extends RouteBuilder {
	
		
	@Override
	public void configure() throws Exception {

		// Set lower timeout for convenience (default 300 seconds).
		getContext().getShutdownStrategy().setTimeout(10);

		// Start some route1 cron scheduled.
		DateTime now = DateTime.now().plusSeconds(2);
		from("quartz2://my_route_quartz_timer?cron="
				+now.getSecondOfMinute()+"+"
				+now.getMinuteOfHour()+"+"
				+now.getHourOfDay()+"+*+*+?")
		.id("scheduling1")
		.to("direct://my-route");
		
		
		// Start other route that shutdowns the context thus shutting down the route1.
		// Simulate the situation where you want to restart your route when there are still
		// ongoing redelivery tasks.
		now = DateTime.now().plusSeconds(4);
		from("quartz2://my_shutdown_route_quartz_timer?cron="
				+now.getSecondOfMinute()+"+"
				+now.getMinuteOfHour()+"+"
				+now.getHourOfDay()+"+*+*+?")
		.id("scheduling2")
		.process(new Processor() {
			
			@Override
			public void process(Exchange arg0) throws Exception {
				getContext().stop();
				
			}
		});
		
		// Do some logic that causes exception and redeliveries for 1 minute.
		from("direct://my-route")
		.id("route1")
		.errorHandler(defaultErrorHandler()
			.maximumRedeliveries(10)
			.redeliveryDelay(1000*6)
			// asyncDelayedRedelivery causes the problem. 
			.asyncDelayedRedelivery()
			.retryAttemptedLogLevel(WARN)
			.retriesExhaustedLogLevel(ERROR))

		.process(new Processor() {
			
			@Override
			public void process(Exchange exchange) throws Exception {
				throw new RuntimeException("Error");
			}
		});
		
	}
	
}
