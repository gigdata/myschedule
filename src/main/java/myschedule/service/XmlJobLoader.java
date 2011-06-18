package myschedule.service;

import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.xml.XMLSchedulingDataProcessor;

/** 
 * Extending the parent class to expose two getter methods.
 * 
 * <p>
 * The XMLSchedulingDataProcessor is more than just a loader, it also keep data after loaded such
 * as list of loaded jobs. This class will expose those fields so user may access them.
 * 
 * <p>
 * If you forget to initialize the ClassLoadHelper parameter before passing to constructor,
 * you will always ended up a xml xsd schema default to the public web version and not the
 * one comes with the quartz jar! So use XmlJobLoader.newInstance() whenever possible.
 *
 * @author Zemian Deng
 */
public class XmlJobLoader extends XMLSchedulingDataProcessor {
		
	public static String XML_SYSTEM_ID = XMLSchedulingDataProcessor.QUARTZ_SYSTEM_ID_JAR_PREFIX;
	
	/** A simple factory method that automatically use CascadingClassLoadHelper as parameter. */
	public static XmlJobLoader newInstance() {
		CascadingClassLoadHelper clhelper = new CascadingClassLoadHelper();
		clhelper.initialize(); // we must initialize this first!
		try {
			return new XmlJobLoader(clhelper);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Failed to construct XmlJobLoader.", e);
		}		
	}
	
	/** The ClassLoadHelper parameter must be initialized before pass in here! */
	public XmlJobLoader(ClassLoadHelper clhelper) throws ParserConfigurationException {
		super(clhelper);
	}
		
	/**
	 * Expose getter with public access.
	 * @return
	 */
	@Override
	public List<JobDetail> getLoadedJobs() {
		return super.getLoadedJobs();
	}
	
	/**
	 * Expose getter with public access.
	 * @return
	 */
	@Override
	public List<Trigger> getLoadedTriggers() {
		return super.getLoadedTriggers();
	}

}