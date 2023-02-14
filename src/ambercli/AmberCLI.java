package ambercli;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jline.console.ConsoleReader;
import jline.console.history.FileHistory;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.tinkerpop.blueprints.Vertex;

import amberdb.AmberDb;
import amberdb.AmberSession;
import amberdb.model.Copy;
import amberdb.model.File;
import amberdb.model.Node;
import amberdb.model.Work;
import static java.lang.System.*;

public class AmberCLI {
	final AmberSession  session;
	
	AmberCLI() {
		MysqlDataSource dataSource = new MysqlDataSource();
		String dbUrl = System.getenv("AMBER_URL");
		String dbUser = System.getenv("AMBER_USER");
		String dbPassword = System.getenv("AMBER_PASS");
		String dossHome = System.getenv("AMBER_PATH");
		if (dbUrl == null || dbUser == null || dbPassword == null || dossHome == null) {
			err.println("Env vars AMBER_URL, AMBER_USER, AMBER_PASS and AMBER_PATH must be set");
			exit(1);
		}
		
		out.print("Connecting to " + dbUrl + " ... ");
		out.flush();
				
		dataSource.setUrl(dbUrl);
		dataSource.setUser(dbUser);
		dataSource.setPassword(dbPassword);
		Path dossPath = Paths.get(dossHome);
		AmberDb db = new AmberDb(dataSource, dossPath);
		session = db.begin();
		
		out.println("OK.\n");
		
	}
	
	void jsShell() {
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine js = mgr.getEngineByMimeType("application/javascript");
		js.put("db", session);
		out.println("AmberDb JavaScript shell. AmberDb session is available as the object 'db'.");
		out.println("Hit Ctrl+D to exit.");
		try {
			ConsoleReader jline = new ConsoleReader();
			for (;;) {
				String line = jline.readLine("js> ");
				if (line == null)
					break;
				try {
					jline.println(js.eval(line).toString());
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	static void usage() {
		out.println("ambercli [command]");
		out.println();
		commands();
		out.println();
		out.println("Launch with no arguments for an interactive shell (much faster than reconnecting every time)");
	}
	
	static void commands() {
		out.println("Commands:");
		out.println("  blob <blobId>   - Find the work (and its parents) which own a blob");
		out.println("  copies <workId> - List copies and blob ids for a work");
		out.println("  info <objId>    - Print object (work, copy etc) information");
		out.println("  js              - launches a JavaScript shell with access to the db");
		out.println("  tree <workId>   - Print the object tree starting at a given work");
	}
	
	void shell() {
		ConsoleReader jline;
		try {
			jline = new ConsoleReader();
			final FileHistory history = new FileHistory(new java.io.File(System.getProperty("user.home"), ".ambercli_history"));
			jline.setHistory(history);
			jline.setHistoryEnabled(true);
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						history.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}					
				}
			}));
			
			for (;;) {
				String line = jline.readLine("amber> ");
				if (line == null)
					break;
				try {
					run(line.split(" +"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	void run(String... args) {
		
		try {
			switch (args[0]) {
			case "blob":
				blob(args[1]);
				break;
			case "copies":
				copies(args[1]);
				break;
			case "info":
				info(args[1]);
				break;
			case "js":
				jsShell();
				break;
			case "shell":
				shell();
				break;
			case "tree":
				tree(args[1]);
				break;
			case "help":
				commands();
				break;
			case "exit":
			case "quit":
			case "bye":
			case "logoff":
			case "logout":
				exit(0);
				break;
			default:
				usage();
				break;
				
			}
		} finally {
			session.rollback();
		}
	}
	
	private void tree(String id) {
		Work work = session.findWork(id);
		tree(work, 0);
	}
	
	void tree(Work work, int depth) {
		for (int i = 0; i < depth; i++)
			out.print("  ");
		printSummary(work);
		for (Work child : work.getChildren()) {
			tree(child, depth + 1);
		}
		for (Copy copy : work.getCopies()) {
			tree(copy, depth + 1);
		}
	}

	private void tree(Copy copy, int depth) {
		for (int i = 0; i < depth; i++)
			out.print("  ");
		printSummary(copy.getFile());		
	}

	void blob(String id) {
		if (id.startsWith("nla.blob-")) {
			id = id.substring("nla.blob-".length());
		}
		long blobId = Long.parseLong(id);
		for (File file: session.getGraph().getVertices("blobId", blobId, File.class)) {
			Copy copy = file.getCopy();
			LinkedList<Work> works = new LinkedList<>();
			{
				Work work = copy.getWork();
				do {
					works.push(work);
					work = work.getParent();
				} while (work != null);
			}
			int depth = 0;
			for (Work work : works) {
				for (int i = 0; i < depth; i++)
					out.print("  ");
				printSummary(work);
				depth++;
			}
			for (int i = 0; i < depth; i++)
				out.print("  ");
			printSummary(file);
			
		}
	}
	
	private void info(String id) {
		Node node = session.findModelObjectById(id, Node.class);
		Vertex v = node.asVertex();
		List<String> keys = new ArrayList<>(v.getPropertyKeys());
		Collections.sort(keys);
		for (String key: keys) {
			String value = ((Object)v.getProperty(key)).toString();
			out.println(key + ": " + value);
		}
	}
	
	void printSummary(Work work) {
		out.println("Work " + work.getObjId() + " [" + work.getType() + "]");
	}
	
	void printSummary(File file) {
		Copy copy = file.getCopy();
		out.println("Copy " + copy.getObjId() + " [" + copy.getCopyRole() + "]: nla.blob-" + file.getBlobId());
	}

	void copies(String id) {
		Work work = session.findWork(id);
		for (Copy copy : work.getCopies()) {
			for (File file : copy.getFiles()) {
				printSummary(file);
			}
		}
		
	}

	public static void main(String args[]) {
		if (args.length == 0) {
			new AmberCLI().run("shell");
			exit(0);
		}
		if (args.length < 1 || "-h".equals(args[0]) || "--help".equals(args[0])) {
			usage();
			exit(1);
		}
		new AmberCLI().run(args);

	}
}
