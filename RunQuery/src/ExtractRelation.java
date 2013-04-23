import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.TreeCoreAnnotations;


public class ExtractRelation {
	StanfordCoreNLP processor;
	
	public ExtractRelation() {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
		processor = new StanfordCoreNLP(props, false);
	}
	
	public String findRelation(String sentence, String ent1, String ent2) {
		Annotation document = new Annotation(sentence);
		processor.annotate(document);
		System.out.println(document.get(SentencesAnnotation.class).get(0).get(TreeCoreAnnotations.TreeAnnotation.class));
		return "";
	}
	
	
}