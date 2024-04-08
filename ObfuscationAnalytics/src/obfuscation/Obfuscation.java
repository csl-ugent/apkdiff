package obfuscation;

public class Obfuscation {
	static String[] reservedKeywords = {"abstract", "assert", "boolean", "break",
			"byte", "case", "catch", "char", "class", "const", "default",
			"do", "double", "else", "enum", "extends", "false", "final",
			"finally", "float", "for", "goto", "if", "implements", "import",
			"instanceof", "int", "interface", "long", "native", "new", "null",
			"package", "private", "protected", "public", "return", "short",
			"static", "strictfp", "super", "switch", "synchronized", "this",
			"throw", "throws", "transient", "true", "try", "void", "volatile",
			"while", "continue"};


	public static boolean isNameMangled(String name){

		//reserved keywords are not valid in java src code but are valid in byrecode
		for(String keyword: reservedKeywords){
			if(name.equals(keyword)){
				return true;
			}
		}

		for (int i = 1; i < name.length(); i++){
		    char c = name.charAt(i);
			if(!Character.isJavaIdentifierPart(c)) return false;
		}

		//Exception R who has special meaning in android
		if(name.equals("R")){
			return false;
		}

		//single/double digit is used for anonymous innner classes/interfaces
		//and not for obfuscation (class cannot start with number)
//		if(className.matches("^\\d$")){
//			return false;
//		}
//		if(className.matches("^\\d\\d$")){
//			return false;
//		}


		//class name of length <=2 (any type of character) except "io" or "id" which is very common name
		if(name.toLowerCase().equals("io")) return false;
		if(name.toLowerCase().equals("id")) return false;
		if(name.length() <= 2) return true;


		//The only allowed characters for identifiers are all alphanumeric
		// characters([A-Z],[a-z],[0-9]), ‘$‘(dollar sign) and ‘_‘ (underscore).
		String pattern = "^[a-zA-Z0-9_\\$]*$";
		if(!name.matches(pattern)) return false;

		//md5 hash
		pattern = "^md5[0-9a-fA-F]{32}$";
		if(name.matches(pattern)) return true;

		//crc hashes
		pattern = "^crc64[0-9a-fA-F]{16}$";
		if(name.matches(pattern)) return true;
		pattern = "^crc32[0-9a-fA-F]{8}$";
		if(name.matches(pattern)) return true;
		pattern = "^crc16[0-9a-fA-F]{4}$";
		if(name.matches(pattern)) return true;
		pattern = "^crc8[0-9a-fA-F]{2}$";
		if(name.matches(pattern)) return true;

		//sha hashes
		pattern = "^sha[0-1]?[0-9a-fA-F]{40}$";
		if(name.matches(pattern)) return true;
		pattern = "^sha(3|224|3-224)?[0-9a-fA-F]{56}$";
		if(name.matches(pattern)) return true;
		pattern = "^sha(3|256|3-256)?[0-9a-fA-F]{64}$";
		if(name.matches(pattern)) return true;
		pattern = "^sha(3|384|3-384)?[0-9a-fA-F]{96}$";
		if(name.matches(pattern)) return true;
		pattern = "^sha(3|512|3-512)?[0-9a-fA-F]{128}$";
		if(name.matches(pattern)) return true;



		//1 - 2  latin letter with possible number after e.g. a, b, c, a0, a1, a2, a12
		pattern = "^[a-zA-Z]{1,2}[0-9]*";
		if(name.matches(pattern)){
			return true;
		}
		//repeating (3 or more) consonants
		pattern = "";
		//backreference does not work here for regex, so old fashioned way with or statements "|"
		for (int i = 0; i < 26; i++) {
		    char ch = (char) ('a' + i);
		    if(ch == 'a' || ch == 'e' || ch == 'i' || ch == 'o' || ch == 'u' ) continue;
		    //consonant
		    pattern += ".*([" + ch + "]){3,}.*|";
		}
		pattern = pattern.substring(0, pattern.length() - 1);
		if(name.matches(pattern)){
			return true;
		}
		return false;
	}

}
