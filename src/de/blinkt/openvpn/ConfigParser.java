package de.blinkt.openvpn;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

//! Openvpn Config FIle Parser, probably not 100% accurate but close enough

// And rember, this is valid :)
// --<foo>
// bar
// </foo>
public class ConfigParser {


	private HashMap<String,Vector<String>> options = new HashMap<String, Vector<String>>();
	private void parseConfig(String filename) throws IOException, ConfigParseError {


		FileReader fr = new FileReader(filename);
		BufferedReader br =new BufferedReader(fr);

		int lineno=0;

		while (true){
			String line = br.readLine();
			if(line==null)
				break;
			lineno++;
			System.out.print("LINE:");
			System.out.println(line);
			Vector<String> args = parseline(line);
			if(args.size() ==0)
				continue;



			if(args.get(0).startsWith("--"))
				args.set(0, args.get(0).substring(2));

			checkinlinefile(args,br);

			options.put(args.get(0), args);
		}
	}

	private void checkinlinefile(Vector<String> args, BufferedReader br) throws IOException, ConfigParseError {
		String arg0 = args.get(0);
		// CHeck for <foo>
		if(arg0.startsWith("<") && arg0.endsWith(">")) {
			String argname = arg0.substring(1, arg0.length()-1);
			String inlinefile = "";

			String endtag = String.format("</%s>",argname);
			do {
				String line = br.readLine();
				if(line==null){
					throw new ConfigParseError(String.format("No endtag </%s> for starttag <%s> found",argname,argname));
				}
				if(line.equals(endtag))
					break;
				else {
					inlinefile+=line;
					inlinefile+= "\n";					
				}
			} while(true);

			args.clear();
			args.add(argname);
			args.add(inlinefile);
		}

	}

	enum linestate {
		initial,
		readin_single_quote
		, reading_quoted, reading_unquoted, done}

	private boolean space(char c) {
		// I really hope nobody is using zero bytes inside his/her config file
		// to sperate parameter but here we go:
		return Character.isSpace(c) || c == '\0';

	}

	public class ConfigParseError extends Exception {
		private static final long serialVersionUID = -60L;

		public ConfigParseError(String msg) {
			super(msg);
		}
	}


	// adapted openvpn's parse function to java
	private Vector<String> parseline(String line) throws ConfigParseError {
		Vector<String> parameters = new Vector<String>(); 

		if (line.length()==0)
			return parameters;


		linestate state = linestate.initial;
		boolean backslash = false;
		char out=0;

		int pos=0;
		String currentarg="";

		do { 
			// Emulate the c parsing ...
			char in;
			if(pos < line.length())
				in = line.charAt(pos);
			else 
				in = '\0';

			if (!backslash && in == '\\' && state != linestate.readin_single_quote)
			{
				backslash = true;
			}
			else
			{
				if (state == linestate.initial)
				{
					if (!space (in))
					{
						if (in == ';' || in == '#') /* comment */
							break;
						if (!backslash && in == '\"')
							state = linestate.reading_quoted;
						else if (!backslash && in == '\'')
							state = linestate.readin_single_quote;
						else
						{
							out = in;
							state = linestate.reading_unquoted;
						}
					}
				}
				else if (state == linestate.reading_unquoted)
				{
					if (!backslash && space (in))
						state = linestate.done;
					else
						out = in;
				}
				else if (state == linestate.reading_quoted)
				{
					if (!backslash && in == '\"')
						state = linestate.done;
					else
						out = in;
				}
				else if (state == linestate.readin_single_quote)
				{
					if (in == '\'')
						state = linestate.done;
					else
						out = in;
				}

				if (state == linestate.done)
				{
					/* ASSERT (parm_len > 0); */
					state = linestate.initial;
					parameters.add(currentarg);
					currentarg = "";
					out =0;
				}

				if (backslash && out!=0)
				{
					if (!(out == '\\' || out == '\"' || space (out)))
					{
						throw new ConfigParseError("Options warning: Bad backslash ('\\') usage");
					}
				}
				backslash = false;
			}

			/* store parameter character */
			if (out!=0)
			{
				currentarg+=out;
			}
		} while (pos++ < line.length());

		return parameters;
	}

	void convertProfile() throws ConfigParseError{
		VpnProfile newprofile = new VpnProfile("converted Profile");
		// Pull, client, tls-client
		
		if(options.containsKey("client") || options.containsKey("pull")) {
			newprofile.mUsePull=true;
			options.remove("pull");
			options.remove("client");
		}
		
		if(options.containsKey("secret")){
			newprofile.mAuthenticationType=VpnProfile.TYPE_STATICKEYS;
			options.remove("secret");
		}
		
		if(options.containsKey("redirect-gateway")) {
			options.remove("redirect-gateway");
			newprofile.mUseDefaultRoute=true;
		} else {
			newprofile.mUseDefaultRoute=true;
		}
		
		Vector<String> mode = options.get("mode");
		if (mode != null){
			options.remove("mode");
			if(mode.size() != 2) 
				throw new ConfigParseError("--mode has more than one parameter");
			if(!mode.get(1).equals("p2p"))
				throw new ConfigParseError("Invalid mode for --mode specified");
		}
		
	}

}




