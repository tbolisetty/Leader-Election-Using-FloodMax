import java.util.Scanner;

public class Master implements Runnable{
	int iGraph[][],iPID[];
	int iNumberOfProcessCompleted;
	int iNumberOfProcesses;
	public Process[] oProc;
	private Scanner in;
	private static String[] sProcessRoundStatus;
	String[] sStatusValues={"RoundCompleted","RoundNotCompleted","ProcessCompleted"};
	String sStatus;
	Thread tMaster;
	Master oMaster;
	boolean bAllProcessesReplied, bInWaitState;

	public Master(){
		//      set the initial status to RoundCompleted
		sStatus=sStatusValues[0];
		//      declare the scanner object
		in=new Scanner(System.in);
		iNumberOfProcessCompleted=0;
		bAllProcessesReplied=false;
		bInWaitState=false;
	}

	public void setMasterObject(Master oMas){
		this.oMaster=oMas;
	}

	public void getInput()
	{
		int iRow, iCount;
		//read number of processes
		System.out.println("Enter number of processes: ");
		iNumberOfProcesses=in.nextInt();
		iCount=iNumberOfProcesses;

		//allocate memory the arrays
		iPID=new int[iNumberOfProcesses];
		iGraph=new int[iNumberOfProcesses][iNumberOfProcesses];
		sProcessRoundStatus=new String[iNumberOfProcesses];
		//		initialize the sProcessRoundStatus
		resetProcessRoundStatus();

		//		get the process id's from user and store it in an array
		System.out.println("Enter the process id's");
		for(iCount=1;iCount<=iNumberOfProcesses;iCount++){
			System.out.print("Process ID of "+iCount+" is: ");
			int x=in.nextInt();
			check:for(int dummy=1;dummy<iCount;dummy++)
			{
				if(iPID[dummy-1]==x)
				{
					System.out.println("please enter unique value");
					x=in.nextInt();
					dummy--;
					continue check;
				}                        
			}
			iPID[iCount-1]=x;
		}
		System.out.println();
		//get the connection matrix
		System.out.println("Enter 1 if there is a connection between Process (X-Y). Otherwise enter 0\n");
		// making a symmetric matrix
		int checkisolatedtree=0;
		for(iRow=1;iRow<=iNumberOfProcesses;iRow++){
			int iCol=0;
			int checkisolatednode=0;
			
	loop:for(iCol=iRow+1;iCol<=iNumberOfProcesses;iCol++)
			{
				//if(iRow!= iCol)
					{
						System.out.print("Process- ("+iPID[iRow-1]+"-"+iPID[iCol-1]+"): ");
						int x=in.nextInt();
						if( x== 1)
							iGraph[iCol-1][iRow-1]=iGraph[iRow-1][iCol-1]=x;
						else
						{
							iGraph[iCol-1][iRow-1]=iGraph[iRow-1][iCol-1]=0;
							checkisolatednode++;
						}
						
					}   
				
				if(checkisolatednode==iNumberOfProcesses-iRow)
					{
					checkisolatedtree++;
					//System.out.println(checkisolatedtree);
						System.out.println("There are no neighbour process node connected for current process");
						System.out.println("Do you want to continue or want to provide any neighbour process? give y/n");				
						//System.out.println("please provide some neighbour for current node");
						String inputvalue=in.next();
						if(inputvalue.compareTo("y")== 0)
						{
							iRow--;
							checkisolatedtree--;
							continue loop;
						}
					}
				if(checkisolatedtree==iNumberOfProcesses-1)
				{
					System.out.println("All nodes in the graph are isolated");
				}
				
					
					
				
			}
			System.out.println();
		} 
		
		//      calling initializeProcess() method
		initalizeProcesses();
	}

	//    Initialize all the processes with their PID and their neighbors
	public void initalizeProcesses(){
		Process[] p;
		oProc=new Process[iNumberOfProcesses];
		int declare_Process=1, iNumberOfNbrs=0, iRow, prs=1;
		int nbrs[][];
		//      initialize the processes
		//       send them their neighbor PID's
		for(declare_Process=1;declare_Process<=iNumberOfProcesses;declare_Process++){
			iNumberOfNbrs=0;
			//count the number of neighbors
			for(prs=1;prs<=iNumberOfProcesses;prs++)
			{
				if(iGraph[declare_Process-1][prs-1]==1){
					iNumberOfNbrs++;
				}
			}
			//initialize the array that will be sent to the Process
			nbrs= new int[iNumberOfNbrs][2];
			iRow=0;
			for(prs=1;prs<=iNumberOfProcesses;prs++)
			{
				if(iGraph[declare_Process-1][prs-1]==1){
					nbrs[iRow][0]=prs;
					nbrs[iRow][1]=iPID[prs-1];
					iRow++;
				}
			}
			//creating the process
			oProc[declare_Process-1]=new Process(iPID[declare_Process-1],declare_Process,nbrs);
		}
		//      send each process about how to communicate to their neighbors
		//      send an array of neighbor objects to the process
		//      first count, how many number of neighbors each process has. Then add each neighbor object to the array and send it to the Process.
		iNumberOfNbrs=0;
		for(declare_Process=1;declare_Process<=iNumberOfProcesses;declare_Process++){
			iNumberOfNbrs=0;
			for(prs=1;prs<=iNumberOfProcesses;prs++)
			{
				if(iGraph[declare_Process-1][prs-1]==1){
					iNumberOfNbrs++;
				}
			}
			p=new Process[iNumberOfNbrs];
			iRow=0;
			for(prs=1;prs<=iNumberOfProcesses;prs++){
				if(iGraph[declare_Process-1][prs-1]==1){
					p[iRow]=oProc[prs-1];
					iRow++;
				}
				
			}
			oProc[declare_Process-1].setNeighborObjects(p);
			//			send the reference of the master to each process
			oProc[declare_Process-1].setMaster(oMaster);
		}

		//@@@- Ask each processes to print their neighbors
		/*for(declare_Process=1;declare_Process<=iNumberOfProcesses;declare_Process++){
			oProc[declare_Process-1].printNeighborPID();
		}*/
		
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			tMaster.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Start of Master thread");
		boolean bRun=false;
		int iCount;
		//		continue this loop until every process is done
		while(!bRun){
			switch(sStatus){
			case "RoundCompleted":
				resetProcessRoundStatus();
				for(iCount=1;iCount<=iNumberOfProcesses;iCount++){
					oProc[iCount-1].processThread("T"+iPID[iCount-1]);
				}
				sStatus=sStatusValues[1];
				break;
			case "RoundNotCompleted":
				readyForNextRound();
				for(iCount=1;iCount<=iNumberOfProcesses;iCount++){
					oProc[iCount-1].printConMatrix();
					oProc[iCount-1].reset("parent");
					oProc[iCount-1].reset("convergeCast");
				}
				/*for(iCount=1;iCount<=iNumberOfProcesses;iCount++){
					oProc[iCount-1].printConMatrix();
				}*/
				break;
			case "ProcessCompleted":
/*				for(iCount=1;iCount<=iNumberOfProcesses;iCount++){
					oProc[iCount-1].printConMatrix();
				}*/
				bRun=true;
			}
		}
		System.out.println("Process terminated");
	}

	private synchronized void readyForNextRound(){
		//    	System.out.println("start function: readyForNextRound()");
		boolean bReadyForNextRound=true, bProcessDone=true;
		int iCount;

		System.out.println("In readyForNextROund()");
		
		if(!bAllProcessesReplied){
			try {
				
				System.out.println("In Master, entering wait state");
				bInWaitState=true;
				wait();
				bAllProcessesReplied=false;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//      if any of the process has not completed their process, break from the loop
		//      and set bReadyForNextRound=False
		//      if any of the process is true, set the bProcessDone=false

		for(iCount=1;iCount<=iNumberOfProcesses;iCount++){
			if(sProcessRoundStatus[iCount-1].compareToIgnoreCase("false")==0){
				bReadyForNextRound=false;
				bProcessDone=false;
				break;
			}
			else if(sProcessRoundStatus[iCount-1].compareToIgnoreCase("true")==0){
				bProcessDone=false;
			}
		}
		//            assign the status of the round completed in sStatus private variable
		if(bProcessDone){
			sStatus=sStatusValues[2];
		}
		else if(bReadyForNextRound){
			sStatus=sStatusValues[0];
			resetProcessRoundStatus();
		}
		else{
			sStatus=sStatusValues[1];
		}
		//        System.out.println("End function: readyForNextRound()");
	}

	public synchronized void setProcessRoundStatus(int iIndex, String sStatus_temp){
		System.out.println("Start of fucntion: setProcessRoundStatus");
		sProcessRoundStatus[iIndex-1]=sStatus_temp;
		System.out.println("End of fucntion: setProcessRoundStatus");
		iNumberOfProcessCompleted++;
		if(iNumberOfProcessCompleted==iNumberOfProcesses){
			if(bInWaitState)
			notify();
		}
	}

	//    reset the sProcessRoundStatus to False at the end of each round
	//    make the static variable to zero
	private void resetProcessRoundStatus() {
		// TODO Auto-generated method stub
		int iCount=1;
		for(iCount=1;iCount<=iNumberOfProcesses;iCount++){
			sProcessRoundStatus[iCount-1]="false";
		}
		iNumberOfProcessCompleted=0;
	}

	public void masterThread(String sThreadName){
		tMaster=new Thread(this, sThreadName);
		tMaster.start();
	}
}