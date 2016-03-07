package jcommand;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;

public class Test {

	public static void main(String[] args) throws Exception {
				
		
		ClassPool pool = ClassPool.getDefault();
		CtClass cc = pool.makeClass("Args");

		//ClassFile cfile = cc.getClassFile();
		//ConstPool cpool = cfile.getConstPool();

		System.out.println(Params.class.getName());
		CtClass paramClass = ClassPool.getDefault().get(Params.class.getName());
		
		//System.out.println(arrListClazz);

		{
			CtField field = new CtField(paramClass, "args", cc);	
			field.getFieldInfo().setAccessFlags(AccessFlag.PUBLIC);
			ConstPool cpool = field.getFieldInfo2().getConstPool();
			AnnotationsAttribute attr = new AnnotationsAttribute(cpool, AnnotationsAttribute.visibleTag);
			Annotation annot = new Annotation(ParametersDelegate.class.getName(),cpool);
			attr.addAnnotation(annot);	
			field.getFieldInfo().addAttribute(attr);
			cc.addField(field);
		}
		
		{
			CtField field = new CtField(paramClass, "args2", cc);	
			field.getFieldInfo().setAccessFlags(AccessFlag.PUBLIC);
			ConstPool cpool = field.getFieldInfo2().getConstPool();
			AnnotationsAttribute attr = new AnnotationsAttribute(cpool, AnnotationsAttribute.visibleTag);
			Annotation annot = new Annotation(ParametersDelegate.class.getName(),cpool);
			attr.addAnnotation(annot);	
			field.getFieldInfo().addAttribute(attr);
			cc.addField(field);
		}
		
		
		Class<?> c = cc.toClass();
		Object o = c.newInstance();
		Field f = o.getClass().getDeclaredField("args");
		Field f2 = o.getClass().getDeclaredField("args2");
		
		Params p = new Params();
		Params p2 = new Params();
		
		f.set(o, p);
		f2.set(o, p2);
		
		
		
		JCommander jc = new JCommander();
		jc.setProgramName("a b");
		jc.usage();
		
		jc.addCommand("hello", o);
		
		
		jc.parse(new String[] {"hello", "-param", "something"});
		
		System.out.println(jc.getParsedCommand());
		
		System.out.println(p.param);

		// Can add subcommands with spaces, with an empty object, or 
		// a javassist object with @Parameters specified.
		
		
	}
	
	public static class GeoWaveOptions {
		@Parameter(names = {"-v", "--verbose"})
		private Boolean verbose;
		
		@Parameter(names = {"-d", "--debug"})
		private Boolean debug;				
	}
	
	public static class StoreCommand {
		@Parameter(names = {"-t", "--type"})
		private String type;
	}
	
	@Parameters(commandDescription = "")
	public static class ConfigCommand {
		
		@Parameter(names = {"-cf", "--config-file"})
		private String configFile;
		
		@Parameter(description = "sub-command")
		private List<String> subCommandParameter;
		// Add command with spaces to the command line
	}

	@Parameters(commandDescription = "Add a store")
	public static class AddStoreCommand {		
		@Parameter
		private List<String> nameParameter;
		
		@ParametersDelegate
		private Object storeOptions;
	}
	
	public static class MemoryOptions {

		@Parameter(names = {"-cs", "--cache-size"})
		private Integer cacheSize;

	}
	
	public static class AccumuloOptions {

		@Parameter(names = {"-i", "--instance"})
		private String instance;
		
		@Parameter(names = {"-z", "--zookeepers"})
		private String zookeepers;
		
		@Parameter(names = {"-u", "--user"})
		private String user;

		@Parameter(names = {"-p", "--password"}, password = true)
		private String password;

	}
	
	
	public static class Params {
		@Parameter(names = {"-param"})
		public String param;
	}
	
	public static void main1(String[] args) {
		
		
		
		List<Command> cc1_sub = new ArrayList<Command>();
		cc1_sub.add(new OptionsCommand("addindex", new GenericOptions()));
		ComposedCommand cc1 = new ComposedCommand("index", cc1_sub);
			
		OptionsCommand kde = new OptionsCommand("kde", new KdeOptions());
				
		List<Command> cc2_sub = new ArrayList<Command>();
		cc2_sub.add(kde);
		ComposedCommand cc2 = new ComposedCommand("analytic", cc2_sub);
		
		List<Command> main_sub = new ArrayList<Command>();
		main_sub.add(cc1);
		main_sub.add(cc2);
		
		ComposedCommand main = new ComposedCommand("main", main_sub);

		Command current = main;
		String[] currentArgs = args;
		
		while (current instanceof ComposedCommand) {

			System.out.println(Arrays.toString(currentArgs));
			
			ComposedCommand compCom = (ComposedCommand)current;

			JCommander jc = new JCommander(current);
			jc.setAcceptUnknownOptions(true);		

			Map<String, Command> commands = new HashMap<String, Command>();
			for (Command command : compCom.getCommands()) {
				commands.put(command.getName(), command);
				jc.addCommand(command.getName(), command);
			}
			
			jc.parse(currentArgs);
			
			String chosenCommand = jc.getParsedCommand();
			System.out.println("Chosen Command: " + chosenCommand);
			if (chosenCommand != null) {
				Command nextCommand = commands.get(chosenCommand);
				
				List<String> additionalCommands = new ArrayList<String>();
								
				if (nextCommand.getAdditionalParameters() != null) {
					additionalCommands.addAll(nextCommand.getAdditionalParameters());
//					currentArgs. = nextCommand.getAdditionalParameters().toArray(currentArgs);
				}
				
				System.out.println(jc.getMainParameter().getNames());
				
				System.out.println(Arrays.toString(jc.getUnknownOptions().toArray()));
				

				additionalCommands.addAll(jc.getUnknownOptions());
				
				currentArgs = additionalCommands.toArray(new String[] {});

				
				current = nextCommand;
			}
			else {
				jc.usage();
				System.exit(0);
			}
		}
		
		// Now parse Command
		Object result = null;
		
		if (current instanceof OptionsCommand) {
			OptionsCommand currOp = (OptionsCommand)current;
			result = currOp.getOptions();
			JCommander jc = new JCommander(currOp.getOptions());
			jc.parse(currentArgs);
		}
		
		if (result instanceof KdeOptions) {
			KdeOptions opts = (KdeOptions)result;
			System.out.println(opts.dbname);
		}
		
		if (result instanceof GenericOptions) {
			GenericOptions opts = (GenericOptions)result;
			System.out.println(opts.genericParameter);
		}
		
	}
	
//	
//	public interface ComposedCommand {
//		String getName();
//		List<ComposedCommand> getCommands();
//		@Parameter(description = "params")
//		List<String> getAdditionalParameters();
//	}
//	

//	public interface Command {
//		String getName();
//	}
//	

	@Parameters(commandDescription = "param..")
	public static class BlahClass {
		
	}
	
	@Parameters(commandDescription = "param..")
	public static abstract class Command {
		private String name;
		
		@Parameter(description = "additional parameters")
		private List<String> additionalParameters = new ArrayList<String>();
		
		protected Command(String name) {
			this.name = name;
		}

		public final String getName() {
			return name;
		}

		public final List<String> getAdditionalParameters() {
			return additionalParameters;
		}
	}
	
	public static final class OptionsCommand extends Command {

		private Object options;
		
		public OptionsCommand(String name, Object options) {
			super(name);
			this.options = options;
		}

		public Object getOptions() {
			return options;
		}
	}
	
	
	
	public static final class ComposedCommand extends Command  {
		
		private List<Command> commands;

		public ComposedCommand(String name, List<Command> commands) {
			super(name);
			this.commands = commands;
		}

		public List<Command> getCommands() {
			return commands;
		}
	}
	
	

//	public List<String> getAdditionalParameters() {
//		
//	}
	
	
//	
//	public static class AnalyticsCommand extends ComposedCommand {
//
//		public String getName() {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		public List<ComposedCommand> getCommands() {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		public List<String> getAdditionalParameters() {
//			// TODO Auto-generated method stub
//			return null;
//		}
//		
//	}
	
	public static class GenericOptions {
		
		@Parameter(names = "-generic")
		public String genericParameter = "default generic name!";
		
	}
	
	public static class KdeOptions {

		@Parameter(names = "-dbname")
		public String dbname = "default db name!";
		
	}
	

}
