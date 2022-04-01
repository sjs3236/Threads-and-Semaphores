/*
  Filename p2.java
  Daate 03/22/2021
  Author Junsik Seo
  Email jxs161930@utdallas.edu
  Description: This project will simulate a visit to the doctor’s office.  It is similar to the “barbershop” example in the textbook.
*/
import java.util.concurrent.Semaphore;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class project2 {
    
    //Semaphores
    private static Semaphore recp_ready = new Semaphore(1);
    private static Semaphore pat_recep = new Semaphore(0);
    private static Semaphore register = new Semaphore(0);
    private static Semaphore pat_wait = new Semaphore(0);
    private static Semaphore nurse_pat = new Semaphore(0);
    private static Semaphore pat_doc = new Semaphore(0);
    private static Semaphore advice = new Semaphore(0);
    private static Semaphore mutex_recep = new Semaphore(1);
    private static Semaphore mutex_nurse = new Semaphore(1);
    private static Semaphore mutex_doc = new Semaphore(1);
    private static Semaphore[] nurse_ready;
    private static Semaphore[] recep_nurse;
    private static Semaphore[] leave;
    private static Semaphore[] closedoor;
    
    //Queues
    private static Queue<Patient> recep_Queue = new LinkedList<>();
    private static Queue<Patient> nurse_Queue = new LinkedList<>();
    private static Queue<Patient> doc_Queue = new LinkedList<>();
    
    public static void main(String[] args) {
	
	//Getting commandline-inputs
	if (Integer.parseInt(args[0])<=0 || Integer.parseInt(args[0])>3){
	    System.out.println("Insufficient input. Try again");
	    System.exit(1);
	}
	
	if (Integer.parseInt(args[1])<=0 || Integer.parseInt(args[1])>30){
	    System.out.println("Insufficient input. Try again");
	    System.exit(1);
	}
	
	//set the patient's numer and doctor's number
	int docNum = Integer.parseInt(args[0]);
        int patNum =Integer.parseInt(args[1]);
	

	
	leave = new Semaphore[patNum];
	nurse_ready = new Semaphore[docNum];
	recep_nurse = new Semaphore[docNum];
	closedoor = new Semaphore[docNum];

        for (int i = 0; i < patNum; i++) {
            leave[i] = new Semaphore(0);
	    
        }
	
	for (int i = 0; i < docNum; i++) {
	    nurse_ready[i] = new Semaphore(1);
	    recep_nurse[i]=new Semaphore(0);
	    closedoor[i]=new Semaphore(0);
        }
	
	//Initialize all threads
        Thread[] patient = new Thread[patNum];
	Thread[] nurse = new Thread[docNum];
	Thread[] doctor = new Thread[docNum];
	
        System.out.println("Run with " + patNum + " patients, " + docNum + " nurses, " + docNum + " doctors");
	
	// Start all threads
	for (int i = 0; i < docNum; i++) {
	    doctor[i] = new Thread(new Doctor(i));
            doctor[i].start();
            nurse[i] = new Thread(new Nurse(i));
            nurse[i].start();
        }
        Thread receptionist = new Thread(new Receptionist());
        receptionist.start();
	
	
        for (int i = 0; i < patNum; i++) {
            patient[i] = new Thread(new Patient(i,docNum));
            patient[i].start();
        }
	
        // release threads
        for (int i = 0; i < patNum; i++) {
            try {
                patient[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
	
        System.exit(0);
    }
    
    
    //	class that simulates a patient
    public static class Patient implements Runnable {
	private int ID;
	private int docNum;
	private int maxdoc;
	
	public Patient(int i, int j ) {
	    ID = i;
	    maxdoc=j;
	}
	
	// Mutators
	public void rand_assign(int i) {
	    this.docNum = i;
	}
	
	// Accessors
	public int patNum() {
	    return ID;
	}
	
	public int docNum() {
	    return docNum;
	}
	
	
	
	public void run() {
	    try {
		//radomy assign doctor foc patient
		Random rand= new Random();
		int bound=maxdoc;
		this.rand_assign(rand.nextInt(bound));
		System.out.println("Patient " + ID + " enters waiting room, waits for receptionist  ");
		Thread.sleep(1000);

		// Wait for a receptionist to be ready
		recp_ready.acquire();
		mutex_recep.acquire();
		// Put patient to receptionist queue
		recep_Queue.add(this);
		mutex_recep.release();
		pat_recep.release();
		register.acquire();
		System.out.println("Patient " + ID + " leaves receptionist and sits in waiting room");
		Thread.sleep(1000);
		pat_wait.release();
		
		// Wait for a nurse to be ready
		nurse_pat.acquire();
		System.out.println("Patient " + ID + " enters doctor's office");
		Thread.sleep(1000);
		
		// Wait for a dcotor to be ready
		pat_doc.release();
		advice.acquire();
		System.out.println("Patient " + ID + " receives advice from doctor "+docNum);
		Thread.sleep(1000);
		System.out.println("Patient " + ID + " leaves");
		Thread.sleep(1000);
		leave[ID].release();
		
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}	
    }
    
    //	class that simulates a receptionist
    public static class Receptionist implements Runnable {
	private Patient pat;
	private int patNum;
	private int docNum;

	public void run() {
	    while (true) {
		try {
		    // Wait for a patient to be ready
		    pat_recep.acquire();
		    mutex_recep.acquire();
		    //Get patient's infomration
		    pat = recep_Queue.poll();
		    patNum = pat.patNum();
		    docNum = pat.docNum();
		    System.out.println("Receptionist registers patient " + patNum);
		    Thread.sleep(1000);
		    mutex_recep.release();
		    //Regsiter a patient
		    register.release();
		    
		    //wait for a patient to enter waiting room
		    pat_wait.acquire();
		    mutex_nurse.acquire();
		    nurse_Queue.add(pat);
		    mutex_nurse.release();
		    //signal a nurse 
		    recep_nurse[docNum].release();
		    
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
	    }
	}
    }
    
    //	class that simulates a nurse
    public static class Nurse implements Runnable {
	private int ID;
	private Patient pat;
	private int patNum;
	
	public Nurse(int docNum) {
	    ID = docNum;
	}
	
	public void run() {
	    while (true) {
		try {
		    //wait signal from the receptionist
		    recep_nurse[ID].acquire();
		    //wait signal from the doctor
		    nurse_ready[ID].acquire();
		    //signal a patient
		    recp_ready.release();
		    mutex_nurse.acquire();
		    //get patient's information
		    pat = nurse_Queue.poll();
		    patNum = pat.patNum();
		    System.out.println("Nurse " + ID + " takes patient " + patNum + " to doctor's office");
		    Thread.sleep(1000);
		    mutex_nurse.release();
		    nurse_pat.release();
		    
		    pat_doc.acquire();
		    mutex_doc.acquire();
		    doc_Queue.add(pat);
		    mutex_doc.release();
		    //signal a doctor
		    closedoor[ID].release();
		    
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
	    }
	}
    }
    
    //	class that simulates a doctor
    public static class Doctor implements Runnable {
	private int ID;
	private Patient pat;
	private int patNum;

	public Doctor (int docNum) {
	    ID = docNum;
	}
	
	public void run() {
	    while (true) {
		try {
		    //wait signal from a nurse
		    closedoor[ID].acquire();
		    mutex_doc.acquire();
		    //get patient's information
		    pat= doc_Queue.poll();
		    patNum = pat.patNum();
		    System.out.println("Doctor " + ID + " listens to symptoms from patient " + patNum);
		    Thread.sleep(1000);
		    mutex_doc.release();
		    advice.release();		
		    leave[patNum].acquire();
		    //signal a nurse that doctor is ready
		    nurse_ready[ID].release();
		    
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
	    }
	}
    }
} 

    
    
    
