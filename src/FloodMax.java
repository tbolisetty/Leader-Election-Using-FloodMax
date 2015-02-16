
public class FloodMax {
	public static void main(String[] args){
		Master m=new Master();
		m.setMasterObject(m);
		m.getInput();
		m.masterThread("master");
	}
}
