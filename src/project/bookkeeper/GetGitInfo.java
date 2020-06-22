package project.bookkeeper;

import java.io.IOException;
import java.time.Instant;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;


public class GetGitInfo {
	
	public static final List <Release> releases= MainControl.releases;
	public static final Repository repository= MainControl.repository;
	public static final List <String> classesList= MainControl.classesList;
		
	
	
	
	 private GetGitInfo() {
		    throw new IllegalStateException("Utility class");
		  }
	
	 
	// PER RECUPERARE FILES PER RELEASE !!!!!
	public static void getFilesPerRelease(Git git, List<Data> dbEntries) throws IOException, GitAPIException {   
		
		Map<RevCommit, Integer> lastCommitRelease= new LinkedHashMap<> ();
		
    	//get Commits
    	Iterable<RevCommit> log = git.log().all().call();
    	List<RevCommit> logCommitList = new  ArrayList<>();
    	
    	for (RevCommit commit : log) {

                logCommitList.add(commit);

        }
    	
    	//=== CONFRONTO DATE COMMIT E DATE RELEASE PER ASSOCIARE A UNA RELEASE TUTTI I SUOI COMMIT
    	
    	for (RevCommit commit : logCommitList) {
    		
    		//prendo un commit e gli associo la release (con confronto date)
    		//poi aggiungo quel commit alla lista di quella release
    		
    		associateCommit(commit);
    		
    	}

    	
    	//=== ADESSO PRENDO L'ULTIMO COMMIT PER OGNI RELEASE
		
       	populateLastCommitRelease(lastCommitRelease);
    	
    	    	
    	// === PRENDO TUTTI I FILE JAVA A PARTIRE DALL'ULTIMO COMMIT DI UNA RELEASE

    	retrieveJavaFiles(dbEntries);

	}	
		
		
		
		
	
	public static void associateCommit(RevCommit commit) {
		
		int i;
		LocalDateTime commitDate;
		LocalDateTime releaseDate;
		
		commitDate= Instant.ofEpochSecond(commit.getCommitTime()).atZone(ZoneId.of("UTC")).toLocalDateTime();
		
		for (i=0;i<releases.size();i++) {
			
			releaseDate=releases.get(i).getDate();
			
			if (commitDate.compareTo(releaseDate)<0) {
				
				//il commit viene prima della data della release res, quindi � dopo la release che ha superato e me lo ritrovo in quella successiva
				
				//resRelease=releases.get(i).getIndex();
				releases.get(i).getCommitsOfRelease().add(commit);
				//System.out.println("release: "+res_release+"      releaseDate: "+releaseDate+"        commitDate: "+commitDate);

				break;
			}
			
		}
		
	}
	
	public static void populateLastCommitRelease(Map<RevCommit, Integer> lastCommitRelease) {
		
		List<RevCommit> commitList;
		RevCommit lastCommit;
		
		//ordina lista commit per data e prendi l'ultima
		
		for (int i=0;i<releases.size();i++) {
			
			commitList=releases.get(i).getCommitsOfRelease();	//lista commit della release
			
			//prendi la lista di commit di quella release

			if (commitList.isEmpty() && i!=0){
				
				//se questa release non ha commit, gli devo mettere come ultimo commit quello della release prima
				releases.get(i).setLastCommit(releases.get(i-1).getLastCommit());
			}
			else {
				for (int j=0;j<commitList.size();j++){
					
					Collections.sort(commitList, (o1, o2) ->  o1.compareTo(o2));
					/*
					//ordina lista di commit per data
			        Collections.sort(commitList, new Comparator<RevCommit>(){
			            //@Override
			            public int compare(RevCommit c1, RevCommit c2) {
			            	
			            	LocalDateTime d1= Instant.ofEpochSecond(c1.getCommitTime()).atZone(ZoneId.of("UTC")).toLocalDateTime();
			            	LocalDateTime d2= Instant.ofEpochSecond(c2.getCommitTime()).atZone(ZoneId.of("UTC")).toLocalDateTime();
			                return d1.compareTo(d2);
			            }
			        });
			        */
			        
			        
			        //System.out.println("release: "+releases.get(i).getIndex()+"     dateCommit: "+commitList.get(j).getAuthorIdent().getWhen());
			        
			        //ora prendo l'ultima data
			        lastCommit=commitList.get(commitList.size()-1);
					releases.get(i).setLastCommit(lastCommit);
					
			        lastCommitRelease.put(lastCommit, releases.get(i).getIndex());
			        
				
				}
			}
	
		}
		
	}

	//prendo TUTTI i file in una release
	public static void retrieveJavaFiles(List<Data> dbEntries) throws IOException {
		
		int count;
		int fileLoc=0;
       

        for (int i=0;i<releases.size();i++) {
        	
        	RevCommit lastCommit= releases.get(i).getLastCommit();
        
            count=0;

            RevTree tree = lastCommit.getTree();
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
            	
            	if (treeWalk.getPathString().contains(".java")) {
            		            		
            		classesList.add(treeWalk.getPathString());
            		releases.get(i).getFilesOfRelease().add(treeWalk.getPathString());
            		
            		count++;
            		
            		Data dbEntry=new Data(releases.get(i), treeWalk.getPathString());
            		fileLoc= Metrics.loc(treeWalk);
            		dbEntry.setLoc(fileLoc);
            		dbEntries.add(dbEntry);
            		
            		//io gli sto passand ogni LastCommit
           		 	//System.out.println("release: "+releases.get(i).getIndex()+"     commits: "+releases.get(i).getCommitsOfRelease().size());

            		//fileLocTouched= Metrics.locTouched(releases.get(i).getCommitsOfRelease());
            		
            		
            		 //System.out.println("count release "+entry.getValue()+": "+count);
                     //System.out.println("release: "+releases.get(i).getIndex()+"     fileLocTouched: "+fileLocTouched+"       file: "+treeWalk.getPathString());
            		 //System.out.println("count: "+count);
	
            		
            	}
            	

            }
            
           
 
         //System.out.println("================================================================");
         //System.out.println("release "+entry.getValue()+"    nFiles: "+releases.get(entry.getValue()-1).getFilesOfRelease().size());
         //System.out.println("count release "+releases.get(i).getIndex()+": "+count);
   		 //System.out.println("count: "+count);

        }
        //System.out.println("count release "+entry.getValue()+": "+count);
	}
	
	
    
    public static List<RevCommit> getCommitsID(Git git, List<Ticket> ticketlist) throws  GitAPIException {
    	
    	List<RevCommit> myCommits= new ArrayList <>();
    	int i;
    	int count=0;
   
    	

    	//get Commits
    	Iterable<RevCommit> log = git.log().call();
    	List<RevCommit> logCommitList = new  ArrayList<>();
    	
    	for (RevCommit commit : log) {
    		
                logCommitList.add(commit);

        }
    	
    	//inserisco in Ticket gli id dei commit che si riferiscono ad esso
    	for (i=0;i<ticketlist.size();i++) {
    		
    		for (RevCommit commit : logCommitList) {
    			
    			if (commit.getFullMessage().contains(ticketlist.get(i).getTicketID()+":")) {
    				
    				ticketlist.get(i).getRelatedCommits().add(commit.getId().toString());

    				myCommits.add(commit);
 
    				count++;   
    				
    			}
    				
    		}
    		
    		
    	}
    	
		//System.out.println("numero commit id +: "+count+"   num tot commit logCommitList: "+logCommitList.size()+"    numero tickets id:"+ticketlist.size());

    	return myCommits;
    	
    }
    
   /*
    public static Boolean compareDates(RevCommit commit) {
    	
    	int i;
    	
    	LocalDateTime dateCommit = Instant.ofEpochSecond(commit.getCommitTime()).atZone(ZoneId.of("UTC")).toLocalDateTime();
		LocalDateTime dateHalfRelease = releases.get(halfRelease-1).getDate();
		
		//System.out.println("date commit: "+dateCommit+"   dateHalfRelease "+dateHalfRelease);

		if (dateCommit.compareTo(dateHalfRelease)<0) {
			//System.out.println("dentro -> date commit: "+dateCommit+"   dateHalfRelease "+dateHalfRelease);
			return true;
		}
		
		return false;
    	
    }
    */


    
    public static void printList(List<String> list) {
    	int i;
    	int len= list.size();
    	
    	for(i=0;i<len;i++) {
    		Log.infoLog("found: " +list.get(i));
    	}
    	
    	Log.infoLog("dim classesList: " +classesList.size());

    }
    	
 

    	 		


}
