package retrieWin.Indexer;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessTrecTextDocument {
	public static List<String> extractRelevantSentences(Collection<TrecTextDocument> documents, String entity1, String entity2) {
		List<String> returnString = new ArrayList<String>();
		for (TrecTextDocument t:documents)
		{
			List<String> currentAllSentences = t.sentences;
			boolean done = true;
			String output = "";
			int firstPos = -1;
			int secondPos = -1;
			for (int j = 0;j<currentAllSentences.size();j++)
			{
				String currentSentence = currentAllSentences.get(j);
			
				
				firstPos = currentSentence.toLowerCase().indexOf(entity1.toLowerCase());
				secondPos = currentSentence.toLowerCase().indexOf(entity2.toLowerCase());
				if (!done && firstPos != 1 && firstPos < secondPos)
				{
					output = "";
					done = true;
				}
				if (!done)
				{
					if (secondPos != -1)
					{
						output = output + "." + currentSentence;
						returnString.add(output);						
					}
					if (firstPos > secondPos)
					{
						output = currentSentence;
						done = false;
					}
					else
					{
						output = "";
						done = true;
					}
				}
				else
				{
					if (firstPos == -1)
						continue;
					else
					{
						output = currentSentence;
						if (secondPos != -1)
						{
							returnString.add(output);
							output = "";
							done = true;
						}
						else
						{
							done = false;
						}
					}
				}
					
			}
		}
		return returnString;
	}
	
	public static List<String> extractRelevantSentences(Collection<TrecTextDocument> documents, String entity1) 
	{
		List<String> returnString = new ArrayList<String>();
		List<String> entitySplits = Arrays.asList(entity1.split(" "));
		for (TrecTextDocument t:documents)
		{
			for(String sentence: t.sentences) {
				for(String currentSentence: getCleanedSentences(sentence))
				{
					String[] colonSeparated = currentSentence.split("(\\s+:\\s+)|\\||\\.{3}");
					for (int sen = 0;sen<colonSeparated.length;sen++)
					{
						for (String entitySplit:entitySplits)
						{
							if (colonSeparated[sen].toLowerCase().contains(entitySplit.toLowerCase()))
							{
								returnString.add(colonSeparated[sen]);
								break;
							}
						}
					}
				}
			}
		}
		return returnString;
	}

	public static Map<String, Set<String>> extractRelevantSentencesWithDocID(Collection<TrecTextDocument> documents, String entity1, boolean phraseQuery) 
	{
		Map<String, Set<String>> returnString = new HashMap<String, Set<String>>();
		List<String> entitySplits = Arrays.asList(entity1.split(" "));
		//System.out.println("Considering entity expansion: " + entity1);
		for (TrecTextDocument t:documents)
		{
			int index = -1;
			for (String sentence: t.sentences)
			{
				index++;
				List<String> cleanedSentences = getCleanedSentences(sentence);
				for(String currentSentence: cleanedSentences) {
					if(currentSentence.length() > 400)
						continue;
					if (phraseQuery)
					{
						if (currentSentence.toLowerCase().contains(entity1))
						{
							if(returnString.containsKey(currentSentence)) 
								returnString.get(currentSentence).add(t.docNumber + "__" + index);
							else
								returnString.put(currentSentence, new HashSet<String>(Arrays.asList(t.docNumber + "__" + index)));
						}
					}
					else
					{
						List<String> tokens = Arrays.asList(currentSentence.toLowerCase().split(" "));
							for (String entitySplit:entitySplits)
							{
	
								if (tokens.contains(entitySplit.toLowerCase()))
								{
									if(returnString.containsKey(currentSentence)) 
										returnString.get(currentSentence).add(t.docNumber + "__" + index);
									else
										returnString.put(currentSentence, new HashSet<String>(Arrays.asList(t.docNumber + "__" + index)));
									break;
								}
	
							}
					}
				}
			}
		}
		return returnString;
	}
	
	public static String deAccent(String str) {
	    String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD); 
	    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	    String s = pattern.matcher(nfdNormalizedString).replaceAll("");
	    return s;
	}
	
	private static double capitalizedContentRatio(String sent) {
		int total = 0, capitalized = 0;
		
		for(String tok: sent.split(" ")) {
			if(!tok.matches("^[a-zA-Z0-9]*$"))
				continue;
			if(tok.matches("^[A-Z].*$")) 
				capitalized++;
			total++;
		}
		
		return (double)capitalized/total;
	}

	public static List<String> getCleanedSentences(String sentence) {
		List<String> results = new ArrayList<String>();
		double capitalizaitionThreshold = 0.7;
		
		//for(String sentence : sentences) {
			//System.out.println("Input :" + sentence);
			//Replacing accented a followed by two upper ascii characters with a '.
			sentence = sentence.replaceAll("â[^\\x00-\\x7f]{2}\\s?", "'");
			//cleaning out upper ascii characters
			sentence = sentence.replaceAll("[Ââ[^\\x00-\\x7f]]+", "");
			sentence = deAccent(sentence);
			//Remove things between {{ }}
			sentence = sentence.replaceAll("\\{\\s*\\{[^\\}]*\\}\\s*\\}","");
			// Handle links inside square brackets
			
			Pattern p = Pattern.compile("\\[\\s*\\[([^\\|\\]]*)\\|([^\\|\\]]*)\\]\\s*\\]");
			Matcher m = p.matcher(sentence);
			while(m.find())
			{
				
				String replacement = m.group(2);
				//System.out.println(replacement);
				sentence = m.replaceFirst(replacement);
				//System.out.println(sentence);
				m.reset(sentence);
			}
			
			sentence = sentence.replaceAll("\\[\\s*\\[", "");
			sentence = sentence.replaceAll("\\]\\s*\\]","");
			
			// Remove (Reuters something something)
			sentence = sentence.replaceAll("\\(\\s*REUTERS[^\\)]*\\)", "");
			sentence = sentence.replaceAll("\\(\\s*Associated Press[^\\)]*\\)", "");
			// Remove (AP something something)
			sentence = sentence.replaceAll("\\(\\s*AP[^\\)]*\\)", "");
			
			// Remove (Getty Images something something)
			sentence = sentence.replaceAll("\\([^(?!GETTY)].*GETTY[^\\)]*\\)", "");
			sentence = sentence.replaceAll("\\([^(?!Getty)].*Getty[^\\)]*\\)", "");
			
			sentence = sentence.replaceAll("Reply [^(?!says)].*says:", "");
			sentence = sentence.replaceAll("^[0-9]+ of [0-9]+","");
			
			List<String> firstS = Arrays.asList(sentence.split("[\\|]+"));
			for (String s:firstS)
			{
				List<String> secondS = Arrays.asList(s.split("\\.{3}"));
				for (String s2:secondS)
				{
					results.addAll(Arrays.asList(s2.split("Reply Retweet Share")));
				}
			}
			
			//System.out.println("Process : "+ sentence);
			//for(String s:))
			//	System.out.println("Output :" + s);
			//results.addAll();
			
		//}
		
		for (Iterator<String> it = results.iterator(); it.hasNext(); )
	        if (capitalizedContentRatio(it.next()) >= capitalizaitionThreshold)
	            it.remove();
		return results;
	}
	
	public static List<String> getCleanedSentences(List<String> sentences) {
		List<String> results = new ArrayList<String>();
		for(String sentence:sentences) {
			results.addAll(getCleanedSentences(sentence));
		}
		return results;
	}
}
