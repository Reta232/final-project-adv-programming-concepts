package com.snhu.app;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.snhu.app.service.StocksDAO;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import static com.snhu.app.util.ExceptionUtil.attempt;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AppStartup
 */
@Component
public class AppStartup implements
	ApplicationListener< ContextRefreshedEvent > {

	@Autowired
	@Qualifier( "appStart" )
	StocksDAO stocksDAO;

	@Autowired
	Logger log;

	@Autowired
	ApplicationContext context;

	@Value("classpath:com/snhu/app/stocks_insert.json")
	Resource stocksInsertJSON;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		CompletableFuture.runAsync( this::doStartupTests );
	}

	/**
	 * Startup tests for the "Wrapper application for your code" portion of the assignment
	 */
	private void doStartupTests(){
		DBObject object;
		try {
			Arrays.stream( context.getResources( "classpath:/com/snhu/app/**" ) )
				.forEach( resource -> log.info( "Resource: {}", resource ) );
			BufferedReader reader = new BufferedReader( new InputStreamReader( stocksInsertJSON.getInputStream() ) );
			object = (DBObject) JSON.parse( reader.lines().collect( Collectors.joining() ) );
		} catch ( Exception e ) {
			log.error( "", e );
			return;
		}

		attempt( log, () -> log.info( "Creating: {}", stocksDAO.create( object ) ) );
		readAndLogTicker();
		attempt( log, () -> log.info( "Updating: {}", stocksDAO.updateVolume( "TEST_TICK", 20053L ) ) );
		readAndLogTicker();
		attempt( log, () -> log.info( "Deleting: {}", stocksDAO.deleteTicker( "TEST_TICK" ) ) );
		readAndLogTicker();
		double from = 0.051D;
		double to = 0.052D;
		attempt( log, () -> log.info( "Reading Avg[ {}, {} ] Count: {}", from, to, stocksDAO.countAveragesFromTo( from, to ) ) );
		String industry = "Medical Laboratories & Research";
		attempt( log, () -> log.info( "Reading Industry ({}): {}", industry, joinResults( stocksDAO.readIndustryTickers( industry ) ) ) );
		String sector = "Healthcare";
		attempt( log, () -> log.info( "Reading Sector ({}): {}", sector, joinResults( stocksDAO.readSharesBySector( sector ) ) ) );
	}

	private void readAndLogTicker(){
		attempt( log, () -> log.info( "Reading:  {}", joinResults( stocksDAO.readTicker( "TEST_TICK" ) ) ) );
	}

	private String joinResults( Stream<?> results ){
		return results.map( Objects::toString ).collect( Collectors.joining( ",\n", "[\n", "\n]") );
	}
}