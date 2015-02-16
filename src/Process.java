
public class Process implements Runnable {
	public int[][] mCon;
	int iMyPID, iNumberOfNbrs, iMaxPID, iRowToFind, iMessagesRcvd_count,iMessagesRcvd_count2;
	int iConvergeCastMsgs_Rcvd, iNumberOfChildren;
	int iParentIndex;
	Thread tProcess;
	Process[] oProc;
	Master oMaster;
	boolean bReadyForTransition, bInWaitStateToTransition, bReadyForTransition2, bInWaitStateToTransition2;
	boolean bInWaitToConvergeCast;
	boolean bReceivedAllConvergeCast;
	int iTempRound;
	String sStatus, sArrStatusValues[], sArrProcesssStatus[], sProcessStatus;
	int[][] arrCoveredID;
	int[][] arrNotCoveredID;
	int[] arrToSendCovered, arrToSendNotCovered;

	//constructor
	Process(int id, int iRowToFind,int nbrs[][]){
		int iRow, iVar;
		//       Assign the Process id- sent by the Master
		iMessagesRcvd_count=iMessagesRcvd_count2=0;
		iMyPID=id;
		iMaxPID=iMyPID;
		iNumberOfNbrs=nbrs.length;
		this.iRowToFind=iRowToFind;
		bReadyForTransition2=bReadyForTransition=false;
		bInWaitStateToTransition=bInWaitStateToTransition2=false;
		iTempRound=0;

		sArrStatusValues=new String[]{"unknown","non-leader","leader","leaf","intermediate"};
		sArrProcesssStatus=new String[]{"false","true","done"};
		sStatus=sArrStatusValues[0];
		sProcessStatus=sArrProcesssStatus[0];

		iConvergeCastMsgs_Rcvd=0;
		bReceivedAllConvergeCast=false;
		bInWaitToConvergeCast=false;
		iNumberOfChildren=0;

		arrCoveredID=new int[iNumberOfNbrs][100];
		arrNotCoveredID=new int[iNumberOfNbrs][100];
		iParentIndex=-1; //this has no parent initially

		//        declare the size of array objects
		oProc=new Process[iNumberOfNbrs];

		System.out.println("Number of neighbors for Process id-"+iMyPID+" is: "+iNumberOfNbrs);

		//initialize the matrix with neighbor rows * 3
		mCon=new int[iNumberOfNbrs][5];

		/*  
		 * Column0 - index of nbrs
		 * Column1 - ID of nbrs
		 * Column2 is- 0 for null, 1-for parent and 2 for child. 
		 * Column3 - converge cast
		 * Column4 - MaxID received by nbrs Initially parent/child is initialized to null and the converge case is initialized to 0 */

		//Copy the information sent by Master to the mCon Matrix
		iRow=1;
		for(iVar=1;iVar<=nbrs.length;iVar++){ 
			mCon[iRow-1][0]=nbrs[iVar-1][0];
			mCon[iRow-1][1]=nbrs[iVar-1][1];
			iRow++;
		}
	}

	public void setNeighborObjects(Process[] p){
		int iCount;
		for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
			oProc[iCount-1]=p[iCount-1];
		}
	}

	//	set the reference to master object
	public void setMaster(Master m){
		this.oMaster=m;
	}

	//    Verify the link with the neighbor and ask them to print their PID's
	public void printNeighborPID()
	{
		int iRow;
		System.out.println("My Process ID is-"+iMyPID);
		System.out.println("My neighbors has-");
		for(iRow=1;iRow<=iNumberOfNbrs;iRow++){ 
			System.out.print("index-" +mCon[iRow-1][0]+"	");
			System.out.print("-PID: "+mCon[iRow-1][1]);
			System.out.println();
			oProc[iRow-1].printPID(iMyPID);
		}
	}

	private void printPID(int iSenserID) {
		// TODO Auto-generated method stub
		System.out.println("My neighbor-"+iSenserID+" called me. My PID is-"+iMyPID);
	}

	@Override
	public void run() {
		System.out.println("start of thread: "+tProcess.getName());
//		iTempRound++;
		
		sendMessage1();
		checkForWait();
		transition1();

		sendMessage2();
		checkForWait2();
		controlConvergeCast();
		
		oMaster.setProcessRoundStatus(iRowToFind, sProcessStatus);
		System.out.println(tProcess.getName()+" is done with a status *"+sProcessStatus);
		resetAll();
	}

	private synchronized void checkForWait() {
		// TODO Auto-generated method stub
		if(iMessagesRcvd_count != iNumberOfNbrs){
			try {
				System.out.println("In-"+tProcess.getName()+"-CheckForWait().bReadyForTransition is-"+bReadyForTransition);
				bInWaitStateToTransition=true;
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		notify();
	}

	private synchronized void checkForWait2() {
		// TODO Auto-generated method stub
		if(iMessagesRcvd_count2 != iNumberOfNbrs){
			try {
				System.out.println("In-"+tProcess.getName()+"-CheckForWait2().bReadyForTransition is-"+bReadyForTransition2);
				bInWaitStateToTransition2=true;
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		notify();
	}

	public void processThread(String sThreadName){
		tProcess=new Thread(this,sThreadName);
		tProcess.start();
		/* try {
			tProcess.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	public void sendMessage1(){

		System.out.println("Start function:sendMessage1(): "+tProcess.getName());
		int iCount;
		for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
			try{
				oProc[iCount-1].receiveMessage(iMyPID,iMaxPID);
			}catch(Exception e){
				try {
					tProcess.sleep(5000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					oProc[iCount-1].receiveMessage(iMyPID,iMaxPID);
					e1.printStackTrace();
				}
			}
		}
		System.out.println("end function:sendMessage1(): "+tProcess.getName());
	}

	public synchronized void receiveMessage(int iSenderID, int iReceivedID){

		System.out.println("Start function- receivedMessage. In"+tProcess.getName()+".Called by neighbor("+iSenderID+")");
		int iCount;
		//    	update the iSenderID in my mCon[row][4] Matrix
		for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
			if(mCon[iCount-1][1]==iSenderID){
				System.out.println("Updating my conMatrix to "+iReceivedID+". My ID is-"+iMyPID);
				mCon[iCount-1][4]=iReceivedID;
				iMessagesRcvd_count++;
				if(iMessagesRcvd_count==iNumberOfNbrs){
					bReadyForTransition=true;
					if(bInWaitStateToTransition){
						notify();
					}
				}
				break;
			}
		}
		System.out.println("End of receiveMessage"+tProcess.getName()+".Called by neighbor("+iSenderID+")");
	}

	public void transition1(){
		//find the max pid
		System.out.println("In transition() function"+tProcess.getName());
		setParent(setMaxID());	
	}

	//	this will find the max id and set the parent as well    
	private int setMaxID(){
		int iCount=1, iSetMaxID=-1, iTempParent=0;
		int iChangeInParent=0; //if 0- no parent, 1-old parent, 2-new parent
		boolean bParentChanged=false;
		System.out.println("In setMaxID-"+tProcess.getName());
		//		if this node has no parent, then the parent values of each neighbor will be -1

		int iTempMaxPID=iMaxPID, iTempParent_temp=0, iNewParent;
		iNewParent=iParentIndex;
		for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
			if(iTempMaxPID < mCon[iCount-1][4]){
				iTempMaxPID=mCon[iCount-1][4];
				iTempParent_temp=iCount;
				bParentChanged=true;
			}
		}

		if(bParentChanged==true){
			iMaxPID=iTempMaxPID;
			iNewParent=iTempParent_temp;
		}
		return iNewParent;
		/*for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
			if(iMaxPID<mCon[iCount-1][4]){
				iMaxPID=mCon[iCount-1][4];
				iTempParent=iCount;
				iChangeInParent=2; //a new parent is found
			}
			else if(iMaxPID==mCon[iCount-1][4]){	
				//	don't change your parent if the max id has not changed
				iSetMaxID=iParentIndex;
				iChangeInParent=1;
			}
		}
		switch(iChangeInParent){
		case 0:
			// this has no parent
			iSetMaxID=-1;
			break;
		case 1:
			//this has the old parent
			break;
		case 2:
			iSetMaxID=iTempParent;
			break;
		default:
			// error encountered
			System.out.println("Error encountered in setMaxID function()-no case found");
		}
	}*/
	}
	private void setParent(int iRow){
		System.out.println("In setParent()"+tProcess.getName());
		// delete the previous parent before setting a new parent
		//		reset("parent");
		if(iRow!=-1){
			iParentIndex=iRow;
			mCon[iRow-1][2]=1;
			System.out.println(tProcess.getName()+" has new parent as PID-"+mCon[iParentIndex-1][1]);
		}
		else{
			iParentIndex=-1;
			System.out.println("I have no parent. I'm a potential leader-"+iMaxPID);
		}		
	}

	void reset(String sColumnToReset){

		//		System.out.println("Start function: reset: "+tProcess.getName());

		int iCount=1, iIndex=4, iValueToSet=-1;
		boolean bShouldWeLoop=false;
		//    	identify which column to reset and to what value it should be reset to
		switch(sColumnToReset){
		case "parent":
			iIndex=2;
			iValueToSet=0;
			bShouldWeLoop=true;
			break;
		case "receivedMaxID":
			iIndex=4;
			iValueToSet=-1;
			bShouldWeLoop=true;
			break;
		case "iMessagesReceived":
			iMessagesRcvd_count=0;
			break;
		case "convergeCast":
			iIndex=3;
			iValueToSet=0;
			break;
		default:
			System.out.println("***Error in reset(). In thread-"+tProcess.getName()+" default value is reached");
			iIndex=4;
			iValueToSet=-1;
		}
		if(bShouldWeLoop){
			for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
				mCon[iCount-1][iIndex]=iValueToSet;
			}
		}
		//		System.out.println("End function: reset: "+tProcess.getName());
	}

	//this sends the accept messages/reject messages to the neighbors
	public void sendMessage2(){

		System.out.println("Start of sendMessage2() of: "+tProcess.getName());
		int iCount=1;
		//send an accept message to it's parent and reject to all
		for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
			if(mCon[iCount-1][2]==1){
				oProc[iCount-1].receiveMessage2("accept",iMyPID);
			}
			else if(mCon[iCount-1][2]==0 || mCon[iCount-1][2]==2){
				oProc[iCount-1].receiveMessage2("reject", iMyPID);
			}
		}

		System.out.println("End function:sendMessage2(): "+tProcess.getName());
		//    	release lock
	}

	//    this function identifies the acknowledgement sent by the neighbor
	//    if reply is "accept", make it as a child. and update mCon to 1 in the corresponding field
	//    if the reply is "reject", make no relation with the neighbor
	public synchronized void receiveMessage2(String sReply, int iSenderID){
		System.out.println("Start function:receiveMessage2(): "+tProcess.getName()+ " called by neighbor-"+iSenderID);
		System.out.println(tProcess.getName()+" received a "+sReply+" message from-"+iSenderID);
		int iCount=1;
		for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
			if(mCon[iCount-1][1]==iSenderID){
				switch(sReply){
				case "accept":
					mCon[iCount-1][2]=2;
					iMessagesRcvd_count2++;
					break;
				case "reject":
					//    				if the max pid is eqaul to the sender pid- then it is your parent.
					//    				hence don't set the relation to null.
					if(!(iParentIndex==iCount)){
						mCon[iCount-1][2]=0;
					}
					iMessagesRcvd_count2++;
					break;
				default:
					System.out.println("**Error- Should receive either an accept/reject message");
				}
				break;
			}
		}

		if(iMessagesRcvd_count2==iNumberOfNbrs){
			bReadyForTransition2=true;
			if(bInWaitStateToTransition2){
				System.out.println("In receiveMessage() of-"+tProcess.getName()+" .Notifying the wait");
				notify();
			}
		}
		System.out.println("end function:receiveMessage2(): "+tProcess.getName());
	}

	//    to print the mCon matrix
	public synchronized void printConMatrix(){
		//		System.out.println("I received "+iMessagesRcvd_count+" messages");
		System.out.println("---"+tProcess.getName()+"---");
	
		int iRow=1, iCol;
		for(iRow=1;iRow<=iNumberOfNbrs;iRow++){
			if(iRow==1)
			{
				System.out.println("");
			}
			System.out.println();
			for(iCol=1;iCol<=5;iCol++){
				System.out.print(mCon[iRow-1][iCol-1]+"		");
			}
		}
	}

	//		This function will reset all the values before the start of the next round
	public void resetAll(){
		System.out.println("In resetAll() of-"+tProcess.getName());
		iMessagesRcvd_count=iMessagesRcvd_count2=0;
		bReadyForTransition=bReadyForTransition2=false;
		bInWaitStateToTransition=bInWaitStateToTransition2=false;
		bInWaitToConvergeCast=bReceivedAllConvergeCast=false;
		iConvergeCastMsgs_Rcvd=0;
		iNumberOfChildren=0;
		sStatus=sArrStatusValues[0];
		sProcessStatus=sArrProcesssStatus[0];
		arrCoveredID=new int[iNumberOfNbrs][100];
		arrNotCoveredID=new int[iNumberOfNbrs][100];
	}

	public void sendConvergeCast(String sReadyForConvergeCast){
		System.out.println("Start of sendConvergeCast in-"+tProcess.getName());
		int iCount=1;
		//		Check if the process is ready to send the ConvergeCast
		for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
			if(mCon[iCount-1][2]==1){
				oProc[iCount-1].receiveConvergeCast(iMyPID, iMaxPID, sReadyForConvergeCast, arrToSendCovered, arrToSendNotCovered);
			}
		}
		System.out.println("End of sendConvergeCast in-"+tProcess.getName());
	}

	public synchronized void receiveConvergeCast(int iSenderID, int iSenderMaxPID, String sReadyForConvergeCast, int[] arrSender_CoveredList, int[] arrSender_NotCoveredList){
		System.out.println("Start receiveConvergeCast() of-"+tProcess.getName()+" called by neighbor-"+iSenderID+". It sent value-"+iSenderMaxPID+" and "+sReadyForConvergeCast+" to me.");
		int iCount, iCopy;
		switch(sReadyForConvergeCast){
		case "yes":
			//		if child has sent the ConvrgerCast but has ID smaller than Parent's ID, do not forward the ConvergeCast
			if(iSenderMaxPID < iMaxPID){
				System.out.println("I received an ID lesser than the Max ID i have seen"+tProcess.getName());
				//		Don't forward this ConvergeCast, hence send a "no" ConvergeCast to your parent
				for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
					if(mCon[iCount-1][1]==iSenderID){
						mCon[iCount-1][3]=0;
						break;
					}
				}
			}
			//		If Child has sent the ConvergeCast and the ID it has sent is equal to the Parent ID, then forward the ConvegeCast
			else if(iSenderMaxPID==iMaxPID){
				System.out.println("I received an ID equal to the Max ID i have seen"+tProcess.getName());
				for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
					if(mCon[iCount-1][1]==iSenderID){
						mCon[iCount-1][3]=1;
						//		Copy the list of ID's sent by the children
						//						System.out.println("It has sent the following id's in covered list");
						//						System.out.println(tProcess.getName()+"-The length of the coverdID sent is-"+arrSender_CoveredList.length);
						for(iCopy=1;iCopy<=arrSender_CoveredList.length;iCopy++){
							if(arrSender_CoveredList[iCopy-1] != 0){
								arrCoveredID[iCount-1][iCopy-1]=arrSender_CoveredList[iCopy-1];
							}
							else{
								break;
							}
						}
						//		Copy all the not covered id list
						for(iCopy=1;iCopy<=arrSender_NotCoveredList.length;iCopy++){
							if(arrSender_NotCoveredList[iCopy-1] != 0){
								arrNotCoveredID[iCount-1][iCopy-1]=arrSender_NotCoveredList[iCopy-1];
							}
						}
						break;
					}
				}
			}
			break;
			//		If child has sent "no", do not forward the ConvergeCasr. So mark 0 in it's corresponding column
		case "no":
			System.out.println("I received a no from my child"+tProcess.getName());
			for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
				if(mCon[iCount-1][1]==iSenderID){
					mCon[iCount-1][3]=0;
					break;
				}
			}
			break;
		default:
			System.out.println("***Error in receiveConvergeCast() of"+tProcess.getName()+". Invalid option is encountered in switch statement");
		}
		//		Increment the number of ConvergeCast messages received
		iConvergeCastMsgs_Rcvd++;
		System.out.println("I received "+iConvergeCastMsgs_Rcvd+" messages so far. And i have "+iNumberOfChildren+" -"+tProcess.getName());
		//		Notify the process if you have received all the ConvergeCast messages and the process is in the waiting state
		if(iNumberOfChildren==iConvergeCastMsgs_Rcvd){
			bReceivedAllConvergeCast=true;
			if(bInWaitToConvergeCast==true){
				System.out.println("I'm Notifying-"+tProcess.getName());
				notify();
			}
		}
		System.out.println("End receiveConvergeCast() of-"+tProcess.getName()+" called by neighbor-"+iSenderID);
	}

	public int findNumberOfChildren(){
		System.out.println("Start of findNumberOfChildren in-"+tProcess.getName());
		int iCount=1,iTempNumberOfChildren=0;
		for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
			if(mCon[iCount-1][2]==2){
				iTempNumberOfChildren++;
			}
		}
		System.out.println("I have "+iTempNumberOfChildren+" children--"+tProcess.getName());
		System.out.println("End of findNumberOfChildren in-"+tProcess.getName());
		return iTempNumberOfChildren;
	}

	public void controlConvergeCast(){
		System.out.println("Start of ConvergeCast() in-"+tProcess.getName());
		iNumberOfChildren=findNumberOfChildren();
		//		If you have no children but have a parent, you are a leaf node. So start the converge cast.
		if(iNumberOfChildren==0 && iParentIndex != -1){
			System.out.println("I'm "+tProcess.getName()+". I'm a leaf");
			//			I'm a leaf
			sStatus=sArrStatusValues[3];

			buildIDList();
			sendConvergeCast("yes");
			setStatus();
		}
		//		If you have children, but not a parent, wait until all the Children send their ConvergeCast
		else if(iNumberOfChildren>0 && iParentIndex != -1){
			System.out.println("I'm "+tProcess.getName()+". I'm no root and no leaf");
			checkForConvergeCast();
			//			I'm an intermediate node
			sStatus=sArrStatusValues[4];
			buildIDList();
			sendConvergeCast(forwardConvergeCast());
			setStatus();

		}
		//		If you have a children but no parent, wait until all children send the convergeCast
		else if(iNumberOfChildren>0 && iParentIndex == -1){
			//		You don't need to send any converge cast
			System.out.println("I'm "+tProcess.getName()+". I'm root");
			//		Setting my status to leader
			sStatus=sArrStatusValues[2];
			checkForConvergeCast();
			buildIDList();
			//			setStatus();
		}
		//		If you neither have a parent nor a child, then mark your Status to done
		else if(iNumberOfChildren==0 && iParentIndex==-1){
			System.out.println("I'm an isolated node");
			setStatus();
			sStatus=sArrStatusValues[0];
		}
		System.out.println("End of ConvergeCast() in-"+tProcess.getName());
	}

	private void buildIDList() {
		System.out.println("Start buildIDList() of"+tProcess.getName());

		int iCount, iCheck, iCoveredID_count=0, iIterate, iNotCoveredID_count=0;
		switch(sStatus){
		case "leaf":
			arrToSendNotCovered=new int[100];
			int iCopyNotCoveredID=0;
			System.out.println(tProcess.getName()+" is in leaf case of buildIDList()");
			arrToSendCovered=new int[1];
			arrToSendCovered[0]=iMyPID;
			arrToSendNotCovered=new int[iNumberOfNbrs-1];
			for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
				if(mCon[iCount-1][2]!=1){
					System.out.println("I'm in leaf of-"+tProcess.getName());
					iCopyNotCoveredID++;
					arrToSendNotCovered[iCopyNotCoveredID-1]=mCon[iCount-1][1];
					System.out.println("Added "+mCon[iCount-1][1]+"=="+arrToSendNotCovered[iCopyNotCoveredID-1]+" to the not covered list"+tProcess.getName());
				}
			}
			break;
		case "intermediate":
			System.out.println(tProcess.getName()+" is in intermediate case of buildIDList()");
			//			printArrayCoveredID();
			//			printArrayUncoveredID();
			//		Count # of covered ID's each of your children has sent
			arrToSendCovered=new int[100];
			arrToSendNotCovered=new int[100];

			//		Fill the array with values
			for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
				for(iIterate=1;iIterate<=100;iIterate++){
					if(arrCoveredID[iCount-1][iIterate-1] != 0){
						iCoveredID_count++;
						arrToSendCovered[iCoveredID_count-1]=arrCoveredID[iCount-1][iIterate-1];
					}
					else{
						break;
					}
				}
			}
			//		It will cover it's own ID, add it to the list of covered ID's
			System.out.println(tProcess.getName()+"# of ID's covered so far is-"+iCoveredID_count);
			iCoveredID_count++;
			arrToSendCovered[iCoveredID_count-1]=iMyPID;

			//		Check # NotCovered ID's received 
			for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
				for(iIterate=1;iIterate<=100;iIterate++){
					if(arrNotCoveredID[iCount-1][iIterate-1] !=0 ){
						boolean bPresentInCoveredList=false;
						//		Check if it is present in the list of covered ID's
						for(iCheck=1;iCheck<=iCoveredID_count;iCheck++){
							if(arrNotCoveredID[iCount-1][iIterate-1]==arrToSendCovered[iCheck-1]){
								bPresentInCoveredList=true;
							}
						}
						if(bPresentInCoveredList==false){
							iNotCoveredID_count++;
							arrToSendNotCovered[iNotCoveredID_count-1]=arrNotCoveredID[iCount-1][iIterate-1];
						}
					}
					else{
						break;
					}
				}
			}
			break;
		case "leader":
			System.out.println(tProcess.getName()+" is in leader case of buildIDList()");
			//			printArrayCoveredID();
			//			printArrayUncoveredID();

			boolean bLeader=true;
			arrToSendCovered=new int[100];
			arrToSendNotCovered=new int[100];

			//		Declare the array of size -# of covered ID + 1 (for it's own ID)
			//		Fill the array with values
			for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
				for(iIterate=1;iIterate<=100;iIterate++){
					if(arrCoveredID[iCount-1][iIterate-1] != 0){
						iCoveredID_count++;
						arrToSendCovered[iCoveredID_count-1]=arrCoveredID[iCount-1][iIterate-1];
					}
					else{
						break;
					}
				}
			}
			//		It will cover it's own ID, add it to the list of covered ID's
			iCoveredID_count++;
			arrToSendCovered[iCoveredID_count-1]=iMyPID;

			for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
				for(iIterate=1;iIterate<=100;iIterate++){
					if(arrNotCoveredID[iCount-1][iIterate-1] !=0 ){
						boolean bPresentInCoveredList=false;
						//		Check if it is present in the list of covered ID's
						for(iCheck=1;iCheck<=iCoveredID_count;iCheck++){
							if(arrNotCoveredID[iCount-1][iIterate-1]==arrToSendCovered[iCheck-1]){
								bPresentInCoveredList=true;
							}
						}
						if(bPresentInCoveredList==false){
							iNotCoveredID_count++;
							arrToSendNotCovered[iNotCoveredID_count-1]=arrNotCoveredID[iCount-1][iIterate-1];
							bLeader=false;
							break;
						}
					}
					else{
						break;
					}
				}
			}
			if(bLeader==true){
				sProcessStatus="done";
			}
			else{
				sProcessStatus="true";
			}
			break;
		}
		printBuildIDList();
		printBuildUncoveredIDLIst();
		System.out.println("End buildIDList() of"+tProcess.getName());
	}

	private void printArrayCoveredID() {
		System.out.println("Start of printArrayCoveredID() in-"+tProcess.getName());
		int iCount=1, iCheck=1;
		for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
			for(iCheck=1;iCheck<=100;iCheck++){
				if(arrCoveredID[iCount-1][iCheck-1] != 0){
					System.out.println(arrCoveredID[iCount-1][iCheck-1]);
				}
			}
		}
		System.out.println("End of printArrayCoveredID() in-"+tProcess.getName());
	}

	void printArrayUncoveredID(){
		System.out.println("Start of printArrayUncoveredID"+tProcess.getName());
		int iCount=1, iCheck=1;
		for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
			for(iCheck=1;iCheck<=100;iCheck++){
				if(arrNotCoveredID[iCount-1][iCheck-1] != 0){
					System.out.println(tProcess.getName()+" has an Uncovered ID-"+arrNotCoveredID[iCount-1][iCheck-1]);
				}
			}
		}
		System.out.println("End of printArrayUncoveredID-"+tProcess.getName());
	}

	private void printBuildIDList() {
		int iCount=1;
		System.out.println(tProcess.getName()+"-My covered list is-");
		for(iCount=1;iCount<=arrToSendCovered.length;iCount++){
			if(arrToSendCovered[iCount-1]!=0){
				System.out.println(tProcess.getName()+"-"+arrToSendCovered[iCount-1]);
			}
		}
	}

	private void printBuildUncoveredIDLIst(){
		System.out.println("Start of printBuildUncoveredIDList() in-"+tProcess.getName());
		int iCount=1;
		for(iCount=1;iCount<=arrToSendNotCovered.length;iCount++){
			if(arrToSendNotCovered[iCount-1] != 0){
				System.out.println(tProcess.getName()+"-"+arrToSendNotCovered[iCount-1]);
			}
		}
		System.out.println("End of printBuildUncoveredIDList() in-"+tProcess.getName());
	}

	public String forwardConvergeCast(){
		System.out.println("Start of ForwardConvergeCast() in-"+tProcess.getName());
		int iCount;
		String sForwardConvergeCast="yes";
		for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
			//		If any of the children has has ID smaller than the greatest ID you have, drop the ConvergeCast
			if(mCon[iCount-1][2]==2){
				if(!(mCon[iCount-1][3]==1)){
					sForwardConvergeCast="no";
					break;
				}
			}
		}
		System.out.println("End of ForwardConvergeCast() in-"+tProcess.getName());
		return sForwardConvergeCast;
	}

	public synchronized void checkForConvergeCast(){
		System.out.println("Start of CheckForConvergeCast() in-"+tProcess.getName());
		if(iNumberOfChildren != iConvergeCastMsgs_Rcvd){
			try{
				bInWaitToConvergeCast=true;
				System.out.println("Entering wait state in-checkConvergeCast() of-"+tProcess.getName());
				wait();
				System.out.println("Out of wait state-"+tProcess.getName());
			}catch (InterruptedException e){
				e.printStackTrace();
			}
		}
		notify();
		System.out.println("End of CheckForConvergeCast() in-"+tProcess.getName());
	}

	//		Check if you have completed the process or just the round
	//		If just the round is completed, mark the status to 'true' and if the process is completed then mark the status to "done"
	public void setStatus(){
		System.out.println("Start of setStatus() in-"+tProcess.getName());
		boolean bDoYouHaveChildren=false, bDoYouHaveAParent=false, bReadyForConvergeCast=false;
		int iCount=0;
		String sReadyForConvergeCast;
		//		check if the process node has a parent and children
		for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
			if(mCon[iCount-1][2]==2){
				bDoYouHaveChildren=true;
			}
			else if(mCon[iCount-1][2]==1){
				bDoYouHaveAParent=true;
			}
		}
		//		if you don't have children but have a parent, you are a leaf node
		if((bDoYouHaveAParent==true)&&(bDoYouHaveChildren==false)){
			sProcessStatus="done";
		}
		//		if you have a parent and children. Check if you all your children have sent converge cast to you
		else if((bDoYouHaveAParent==true)&&(bDoYouHaveChildren==true)){
			sProcessStatus="done";
			for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
				//		If any of the children has not sent the ConvergeCast, then you are not ready for the ConvergeCast
				if(mCon[iCount-1][2]==2){
					if(!(mCon[iCount-1][3]==1)){
						sProcessStatus="true";
						break;
					}
				}
			}
		}
		//		If you don't have a parent but have children, check if you have received ConvergeCast from all the children
		//		If yes, mark the status as done, otherwise mark it as true
		else if(bDoYouHaveAParent==false && bDoYouHaveChildren==true){
			sProcessStatus="done";
			for(iCount=1;iCount<=iNumberOfNbrs;iCount++){
				if(mCon[iCount-1][2]==2){
					if(!(mCon[iCount-1][3]==1)){
						sStatus="true";
					}
				}
			}
		}
		else if(bDoYouHaveAParent==false && bDoYouHaveChildren==false){
			sProcessStatus="true";
		}
		System.out.println("End of setStatus in-"+tProcess.getName());
	}

}