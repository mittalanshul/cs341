package retrieWin.SSF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrieWin.Indexer.Indexer;
import retrieWin.Indexer.TrecTextDocument;
import retrieWin.SSF.Constants.EntityType;
import retrieWin.SSF.Constants.NERType;
import retrieWin.SSF.Constants.SlotName;
import retrieWin.Utils.FileUtils;
import retrieWin.Utils.Utils;

public class SSF {
	List<Slot> slots;
	List<Entity> entities;
	
	public SSF() {
		initialize();
	}
	
	public void initialize() {
		readEntities();
		readSlots();
	}
	
	@SuppressWarnings("unchecked")
	public void readEntities() {
		File file = new File(Constants.entitiesSerilizedFile);
		if(file.exists()) {
			entities = (List<Entity>)FileUtils.readFile(file.getAbsolutePath().toString());
		}
		else {
			String fileName = "data/entities.csv";
			entities = new ArrayList<Entity>();
			try {
				BufferedReader reader = new BufferedReader(new FileReader(fileName));
				String line = "";
				while((line = reader.readLine()) != null) {
					String[] splits = line.split("\",\"");
					EntityType type = splits[1].replace("\"", "").equals("PER") ? EntityType.PER : (splits[1].equals("ORG") ? EntityType.ORG : EntityType.FAC);
					String name = splits[0].replace("\"", "").replace("http://en.wikipedia.org/wiki/", "").replace("https://twitter.com/", "");
					Entity entity = new Entity(splits[0].replace("\"", ""), name, type, splits[2].replace("\"", ""),
							Utils.getEquivalents(splits[3].replace("\"", "")), getDisambiguations(name));
					entities.add(entity);
				}
				reader.close();
			}
			catch (Exception ex) {
				System.out.println(ex.getMessage());
			}
			FileUtils.writeFile(entities, Constants.entitiesSerilizedFile);
		}
		/* for(Entity e:entities) {
			System.out.println(e.getName());
			System.out.println(e.getTargetID());
			System.out.println(e.getGroup());
			System.out.println(e.getEntityType());
			System.out.println(e.getExpansions());
			System.out.println(e.getDisambiguations());
		} */
	}
	
	public List<String> getDisambiguations(String entity) {
		String baseFolder = "data/entities_expanded/";
		List<String> disambiguations = new ArrayList<String>();
		
		try {
			File file = new File(baseFolder + entity + ".expansion");
			System.out.println(file.getAbsolutePath());
			if(file.exists()) {
				System.out.println("file exists");
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = "";
				while((line = reader.readLine()) != null) {
					disambiguations.add(line.trim());
				}
				reader.close();
			}
		}
		catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		return disambiguations;
	}
	
	@SuppressWarnings("unchecked")
	public void readSlots() {
		File file = new File(Constants.slotsSerializedFile);
		if(file.exists()) {
			slots = (List<Slot>)FileUtils.readFile(file.getAbsolutePath().toString());
		}
		else {
			String inputFileName = "data/Slots.csv";
	
			try {
					List<Slot> slots = new ArrayList<Slot>();
					BufferedReader reader = new BufferedReader(new FileReader(inputFileName));
					String line = "";
					while((line = reader.readLine()) != null) {
						String[] vals = line.trim().split(",");
						Slot slot = new Slot();
						slot.setName(SlotName.valueOf(vals[0]));
						slot.setEntityType(EntityType.valueOf(vals[1]));
						slot.setThreshold(Double.parseDouble(vals[2]));
						slot.setApplyPatternAfterCoreference(Integer.parseInt(vals[3]) == 1 ? true : false);
						slot.setPatterns(new ArrayList<SlotPattern>());
						List<NERType> targetNERTypes = new ArrayList<NERType>();
						for(String ner: vals[4].split("\\$")) {
							targetNERTypes.add(NERType.valueOf(ner));
						}
						slot.setTargetNERTypes(targetNERTypes);
						
						if(slot.getEntityType() == EntityType.FAC)
							slot.setSourceNERTypes(Arrays.asList(NERType.ORGANIZATION, NERType.LOCATION));
						else if(slot.getEntityType() == EntityType.PER)
							slot.setSourceNERTypes(Arrays.asList(NERType.PERSON));
						else
							slot.setSourceNERTypes(Arrays.asList(NERType.ORGANIZATION));
						slots.add(slot);
					}
					reader.close();
					FileUtils.writeFile(slots, Constants.slotsSerializedFile);
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		/* for(Slot slot: slots)
			System.out.println(slot.getName() + "," + slot.getEntityType() + "," + slot.getSourceNERTypes() + "," + slot.getTargetNERTypes() + "," + slot.getThreshold());
		//System.out.println(slots); */
	}
	
	public Boolean VerifyIndexExistence(String timestamp)
	{
		String s3IndexFolder = Constants.s3directory + timestamp;
		File f = new File(s3IndexFolder);
		if (!f.exists())
			return false;
		
		File[] subdirectories = f.listFiles();
		Set<String> subdirectoryNames = new HashSet<String>();
		for (File subdir:subdirectories)
			subdirectoryNames.add(subdir.getName());
		return (subdirectoryNames.contains("index") && subdirectoryNames.contains("filteredSerialized.ser"));
	}
	
	public void writeIndexToS3fs(String timestamp)
	{
		try{
		String s3directory = Constants.s3directory+timestamp + "/";
		String s3cmdCommand = String.format("sudo mkdir %s;sudo cp -r %s %s",s3directory,Constants.indexLocation,s3directory);
		Process p;
		p = Runtime.getRuntime().exec(s3cmdCommand);
		p.waitFor();
		
		String s3cmdSerializedFileCopyCommand = String.format("sudo cp %s %s",Constants.trecTextSerializedFile,s3directory);
		p = Runtime.getRuntime().exec(s3cmdSerializedFileCopyCommand);
		p.waitFor();
		}
		catch (Exception e)
		{
			System.out.println("Writing to S3 failed");
			e.printStackTrace();
		}
	}
	
	public void readIndexFromS3fs(String timestamp)
	{
		try{
		String s3directory = Constants.s3directory+timestamp + "/";
		String s3cmdCommand = String.format("sudo cp -r %s%s %s",s3directory,Constants.indexLocation,Constants.indexLocation);
		Process p;
		p = Runtime.getRuntime().exec(s3cmdCommand);
		p.waitFor();
		
		String s3cmdSerializedFileCopyCommand = String.format("sudo cp %s%s %s",s3directory,Constants.trecTextSerializedFile,Constants.trecTextSerializedFile);
		p = Runtime.getRuntime().exec(s3cmdSerializedFileCopyCommand);
		p.waitFor();
		}
		catch (Exception e)
		{
			System.out.println("Reading from S3 failed");
			e.printStackTrace();
		}	
	}
	
	public void runSSF(String timestamp) {
		/*System.out.println("Inside");
		IndriIndexBuilder.buildIndex("/home/aju/cs341/data/smallIndex", "/home/aju/cs341/data/doc");
		System.out.println("Done");*/
		/** create index for the current hour */
		File tempDir = new File("temp");
		// if the directory does not exist, create it
		if (!tempDir.exists())
		    tempDir.mkdir(); 
		Boolean doesIndexExist = VerifyIndexExistence(timestamp);
		if (!doesIndexExist)
		{
			Indexer.createIndex(timestamp, Constants.workingDirectory, Constants.indexLocation, Constants.trecTextSerializedFile, entities); 
			writeIndexToS3fs(timestamp);
		}
		else
		{
			readIndexFromS3fs(timestamp);
		}
		/*
		for(Entity ent: entities) {
			Map<TrecTextDocument,Double> docs= ent.getRelevantDocuments(Constants.indexLocation);

			for(Slot slot: slots) {
				List<String> candidateVals = slot.extractSlotVals(ent, docs);
				List<String> updatedVals = ent.updateSlot(slot, candidateVals);
				if(!updatedVals.isEmpty())
					System.out.println(slot.getName() + " updated with " + updatedVals);
				else
					System.out.println(slot.getName() + " not updated");
			}
		}
		*/
	}
	
	public static void main(String[] args) {
		//createSlotsFromFile("data/Slots.csv", "data/SlotPatterns");
		//List<Slot> slots = (List<Slot>) FileUtils.readFile("data/SlotPatterns");
		//for(Slot slot: slots)
		//System.out.println(slot.getName() + "," + slot.getEntityType() + "," + slot.getSourceNERTypes() + "," + slot.getTargetNERTypes() + "," + slot.getThreshold());
		//System.out.println(slots);
		System.out.println(args[0]);
		new SSF().runSSF(args[0]);
	}
}
