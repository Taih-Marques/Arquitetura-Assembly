package architecture;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import assembler.Assembler;
import components.Bus;
import components.Memory;
import components.Register;
import components.Ula;

public class Architecture {

	private boolean simulation; // this boolean indicates if the execution is done in simulation mode.
								// simulation mode shows the components' status after each instruction

	private boolean halt;
	private Bus extbus1;
	private Bus intbus1;
	private Bus intbus2;
	private Memory memory;
	private int memorySize;
	private Register PC;
	private Register IR;
	private Register RPG;
	private Register RPG1;
	private Register RPG2;
	private Register RPG3;
	private Register Flags;
	private Ula ula;
	private Bus demux; // only for multiple register purposes

	private ArrayList<String> commandsList;
	private ArrayList<Register> registersList;

	private final int INICIO_AREA_RESERVADA = 200;

	/**
	 * Instanciates all components in this architecture
	 */
	private void componentsInstances() {
		// don't forget the instantiation order
		// buses -> registers -> ula -> memory
		extbus1 = new Bus();
		intbus1 = new Bus();
		intbus2 = new Bus();
		PC = new Register("PC", extbus1, null);
		IR = new Register("IR", extbus1, intbus2);
		RPG = new Register("RPG0", extbus1, intbus1);
		RPG1 = new Register("RPG1", extbus1, intbus1);
		RPG2 = new Register("RPG2", extbus1, intbus1);
		RPG3 = new Register("RPG3", extbus1, intbus1);
		Flags = new Register(3, intbus1);
		fillRegistersList();
		ula = new Ula(intbus1, intbus2);

		memorySize = 256;
		memory = new Memory(memorySize, extbus1);
		fillReservedSpace();

		demux = new Bus(); // this bus is used only for multiple register operations

		fillCommandsList();
	}

	/**
	 * This method fills the registers list inserting into them all the registers we
	 * have.
	 * IMPORTANT!
	 * The first register to be inserted must be the default RPG
	 */
	private void fillRegistersList() {
		registersList = new ArrayList<Register>();
		registersList.add(RPG);
		registersList.add(RPG1);
		registersList.add(RPG2);
		registersList.add(RPG3);
		registersList.add(PC);
		registersList.add(IR);
		registersList.add(Flags);
	}

	/**
	 * Constructor that instanciates all components according the architecture
	 * diagram
	 */
	public Architecture() {
		componentsInstances();

		// by default, the execution method is never simulation mode
		simulation = false;
	}

	public Architecture(boolean sim) {
		componentsInstances();

		// in this constructor we can set the simoualtion mode on or off
		simulation = sim;
	}

	// getters

	protected Bus getExtbus1() {
		return extbus1;
	}

	protected Bus getIntbus1() {
		return intbus1;
	}

	protected Bus getIntbus2() {
		return intbus2;
	}

	protected Memory getMemory() {
		return memory;
	}

	protected Register getPC() {
		return PC;
	}

	protected Register getIR() {
		return IR;
	}

	protected Register getRPG() {
		return RPG;
	}

	protected Register getRPG1() {
		return RPG1;
	}

	protected Register getRPG2() {
		return RPG2;
	}

	protected Register getRPG3() {
		return RPG3;
	}

	protected Register getFlags() {
		return Flags;
	}

	protected Ula getUla() {
		return ula;
	}

	public ArrayList<String> getCommandsList() {
		return commandsList;
	}

	// all the microprograms must be impemented here
	// the instructions table is
	/*
	 *
	 * add addr (rpg <- rpg + addr)
	 * sub addr (rpg <- rpg - addr)
	 * jmp addr (pc <- addr)
	 * jz addr (se bitZero pc <- addr)
	 * jn addr (se bitneg pc <- addr)
	 * read addr (rpg <- addr)
	 * store addr (addr <- rpg)
	 * ldi x (rpg <- x. x must be an integer)
	 * inc (rpg++)
	 * move regA regB (regA <- regB)
	 */

	protected void fillReservedSpace() {
		List<Integer> imul = Arrays.asList(
			12, -1, 2,
			0, 2, 0,
			17, 220,
			1, 201, 1,
			15, 210,
			10, 1, 206,
			9, 200, 0,
			9, 201, 1,
			9, 202, 2,
			9, 203, 3,
			9, 205, 5,
			9, 206, -1,
			9, 204, 4
		);
		for (int i = 0; i < imul.size(); i++) {
			int endereco = INICIO_AREA_RESERVADA + 7 + i;
			extbus1.put(endereco);
			memory.store();
			extbus1.put(imul.get(i));
			memory.store();
		}
	}

	/**
	 * This method fills the commands list arraylist with all commands used in this
	 * architecture
	 */
	protected void fillCommandsList() {
		commandsList = new ArrayList<String>();

		commandsList.add("addRegReg"); // 0
		commandsList.add("addMemReg"); // 1
		commandsList.add("addRegMem"); // 2
		commandsList.add("subRegReg"); // 3
		commandsList.add("subMemReg"); // 4
		commandsList.add("subRegMem"); // 5
		commandsList.add("imulMemReg"); // 6
		commandsList.add("imulRegMem"); // 7
		commandsList.add("imulRegReg"); // 8
		commandsList.add("moveMemReg"); // 9
		commandsList.add("moveRegMem"); // 10
		commandsList.add("moveRegReg"); // 11
		commandsList.add("moveImmReg"); // 12
		commandsList.add("incReg"); // 13
		commandsList.add("incMem"); // 14
		commandsList.add("jmp"); // 15
		commandsList.add("jn"); // 16
		commandsList.add("jz"); // 17
		commandsList.add("jnz"); // 18
		commandsList.add("jeq"); // 19
		commandsList.add("jgt"); // 20
		commandsList.add("jlw"); // 21
		commandsList.add("ldi"); // 22
		commandsList.add("read"); // 23
		commandsList.add("store"); // 24
	}

	/**
	 * This method is used after some ULA operations, setting the flags bits
	 * according the result.
	 * 
	 * @param result is the result of the operation
	 *               NOT TESTED!!!!!!!
	 */
	private void setStatusFlags(int result) {
		Flags.setBit(0, 0);
		Flags.setBit(1, 0);
		Flags.setBit(2, 0);
		if (result == 0) { // bit 0 in flags must be 1 in this case
			Flags.setBit(0, 1);
		}
		if (result < 0) { // bit 1 in flags must be 1 in this case
			Flags.setBit(1, 1);
		}
		if (result != 0) { // bit 2 in flags must be 1 in this case
			Flags.setBit(2, 1);
		}
	}

	public void incrementarPC() {
		PC.read();
		IR.store();
		IR.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store(); // now PC points to the first parameter (the first reg id)
		// fim inc
	}

	public void addRegReg() {
		incrementarPC();

		// PC.read();
		memory.read(); // the first register id is now in the external bus.
		demux.put(extbus1.get());
		demuxRegisterInternalRead();
		ula.store(0);

		incrementarPC();

		memory.read(); // the second register id is now in the external bus.

		demux.put(extbus1.get());
		demuxRegisterInternalRead();
		ula.store(1);

		ula.add();
		ula.read(1);
		setStatusFlags(intbus1.get());
		demuxRegisterInternalStore();
		incrementarPC();
	}

	public void addMemReg() {
		incrementarPC();

		memory.read();
		memory.read(); // valor da variavel inserida no extbus 1
		demux.put(extbus1.get());
		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store(); // now PC points to the second parameter (the reg id)
		extbus1.put(demux.get());
		IR.store();
		IR.internalRead();
		ula.internalStore(0);
		PC.read();
		memory.read();

		demux.put(extbus1.get()); // points to the correct register
		demuxRegisterInternalRead(); // starts the read from the register identified into demux bus
		ula.store(1);
		ula.add();
		ula.read(1);

		setStatusFlags(intbus1.get());
		demuxRegisterInternalStore(); // performs an internal store for the register identified into demux bus
		incrementarPC();
	}

	public void addRegMem() {
		incrementarPC();
		memory.read(); // the register id is now in the external bus.
		demux.put(extbus1.get());

		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store(); // now PC points to the second parameter
		memory.read();
		memory.store(); // endereço do segundo parametro guardado para store do resultado posteriormente
		memory.read(); // le o valor do segundo parametro

		demuxRegisterInternalRead(); // valor do reg no intbus1
		ula.store(0); // valor do intbus1 na ula
		IR.store();
		IR.internalRead();
		ula.internalStore(1);
		ula.add();

		ula.read(1);
		setStatusFlags(intbus1.get());

		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		memory.store();

		incrementarPC();
	}

	public void subRegReg() {
		incrementarPC();

		memory.read(); // the first register id is now in the external bus.
		demux.put(extbus1.get()); // points to the correct register
		demuxRegisterInternalRead(); // starts the read from the register identified into demux bus
		ula.store(0);

		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store(); // now PC points to the second parameter (the second reg id)

		memory.read(); // the second register id is now in the external bus.
		demux.put(extbus1.get());// points to the correct register
		demuxRegisterInternalRead();
		ula.store(1);
		ula.sub();

		ula.read(1);
		setStatusFlags(intbus1.get());
		demuxRegisterInternalStore(); // performs an internal store for the register identified into demux bus

		incrementarPC();
	}

	public void subMemReg() {
		incrementarPC();

		memory.read();
		memory.read();
		demux.put(extbus1.get());

		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store(); // now PC points to the second parameter (the second reg id)

		extbus1.put(demux.get());
		IR.store();
		IR.internalRead();
		ula.internalStore(0);

		PC.read();
		memory.read();// ID do reg no extbus1
		demux.put(extbus1.get()); // points to the correct register
		demuxRegisterInternalRead(); // starts the read from the register identified into demux bus
		ula.store(1);

		ula.sub();
		ula.read(1);
		setStatusFlags(intbus1.get());
		demuxRegisterInternalStore(); // performs an internal store for the register identified into demux bus

		incrementarPC();
	}

	public void subRegMem() {
		incrementarPC();

		memory.read(); // the register id is now in the external bus.
		demux.put(extbus1.get()); // points to the correct register

		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store(); // now PC points to the second parameter (the second reg id)

		demuxRegisterInternalRead(); // starts the read from the register identified into demux bus
		ula.store(0);

		memory.read();
		memory.store();
		memory.read();
		IR.store();
		IR.internalRead();
		ula.internalStore(1);
		ula.sub();

		ula.read(1);
		setStatusFlags(intbus1.get());

		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		memory.store();

		incrementarPC();
	}

	public void imulMemReg() {
		incrementarPC();

		memory.read();
		memory.read();
		demux.put(extbus1.get());

		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store();

		extbus1.put(demux.get());
		IR.store();
		IR.internalRead();

		PC.read();
		memory.read();
		demux.put(extbus1.get());
		demuxRegisterInternalRead();
		ula.internalStore(1);

		extbus1.put(INICIO_AREA_RESERVADA + 8);
		PC.store();

		ula.read(1);
		demuxRegisterInternalStore();

		setStatusFlags(intbus1.get());

		incrementarPC();
	}

	public void imulRegMem() {
		incrementarPC();

		memory.read();
		demux.put(extbus1.get());

		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store();

		demuxRegisterInternalRead();
		ula.store(0);

		memory.read();
		memory.store();
		memory.read();

		IR.store();
		IR.internalRead();
		ula.internalStore(1);

		extbus1.put(INICIO_AREA_RESERVADA + 8);
		PC.store();

		ula.read(1);
		setStatusFlags(intbus1.get());

		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		memory.store();

		incrementarPC();
	}

	public void imulRegReg() {
		incrementarPC();

		memory.read();
		demux.put(extbus1.get());
		demuxRegisterInternalRead();//primeiro parametro no intbus1

		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store();

		memory.read();
		demux.put(extbus1.get());

		incrementarPC();
		salvarEstadoRegistradores();
		// testar se os valores são negativos, ja que nao pode usar -1 pra iterar nesse
		// caso
		//RPG.internalStore(); // ta recebendo 9 por algum motivo
		
		demuxRegisterInternalRead();
		RPG1.internalStore();


		// Salvar destino multiplicacao
		int regId = demux.get();
		extbus1.put(INICIO_AREA_RESERVADA + 40);
		memory.store();
		extbus1.put(regId);
		memory.store();

		extbus1.put(INICIO_AREA_RESERVADA + 7);
		PC.store();
	}

	public void salvarEstadoRegistradores() {
		registersList.subList(0, 6).forEach(reg -> {
			int endereco = INICIO_AREA_RESERVADA + registersList.indexOf(reg);
			extbus1.put(endereco);
			memory.store();
			reg.read();
			memory.store();
		});
	}

	public void moveRegReg() {
		incrementarPC();

		memory.read(); // the first register id is now in the external bus.
		demux.put(extbus1.get());

		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store(); // now PC points to the second parameter (the second reg id)

		demuxRegisterInternalRead();

		memory.read();
		demux.put(extbus1.get()); // points to the correct register
		demuxRegisterInternalStore(); // starts the read from the register identified into demux bus

		incrementarPC();
	}

	public void moveMemReg() {
		incrementarPC();

		memory.read();
		memory.read();

		demux.put(extbus1.get());
		incrementarPC();
		extbus1.put(demux.get());
		IR.store();
		
		PC.read();
		memory.read();
		demux.put(extbus1.get()); // points to the correct register
		IR.read();

		demuxRegisterStore(); // performs an internal store for the register identified into demux bus

		incrementarPC();
	}

	public void moveRegMem() {
		incrementarPC();

		memory.read();
		demux.put(extbus1.get()); // points to the correct register

		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store(); // now PC points to the second parameter (the reg id)

		memory.read();
		memory.store();
		demuxRegisterRead();
		memory.store();

		incrementarPC();
	}

	public void moveImmReg() {
		incrementarPC();

		memory.read();
		demux.put(extbus1.get());

		incrementarPC();

		extbus1.put(demux.get());
		IR.store();

		PC.read();
		memory.read();
		demux.put(extbus1.get()); // points to the correct register
		IR.read();
		demuxRegisterStore(); // performs an internal store for the register identified into demux bus

		incrementarPC();
	}

	public void incReg() {
		incrementarPC();

		memory.read();
		demux.put(extbus1.get());
		demuxRegisterInternalRead();
		ula.store(1);
		ula.inc();
		ula.read(1);
		setStatusFlags(intbus1.get());
		demuxRegisterInternalStore();

		incrementarPC();
	}

	public void incMem() {
		incrementarPC();

		memory.read();
		memory.store();
		memory.read();

		IR.store();
		IR.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.read(1);
		setStatusFlags(intbus1.get());

		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		memory.store();

		incrementarPC();
	}

	public void jmp() {
		incrementarPC();
		memory.read();
		PC.store();
	}

	public void jz() {
		incrementarPC();

		if (Flags.getBit(0) == 1) {
			memory.read();
			PC.store();
		} else {
			ula.inc();
			ula.internalRead(1);
			IR.internalStore();
			IR.read();
			PC.store();
		}
	}

	public void jn() {
		incrementarPC();
		;

		if (Flags.getBit(1) == 1) {
			memory.read();
			PC.store();
		} else {
			ula.inc();
			ula.internalRead(1);
			IR.internalStore();
			IR.read();
			PC.store();
		}
	}

	public void jnz() {
		incrementarPC();

		if (Flags.getBit(2) == 1) {
			memory.read();
			PC.store();
		} else {
			ula.inc();
			ula.internalRead(1);
			IR.internalStore();
			IR.read();
			PC.store();
		}
	}

	public void jeq() {
		incrementarPC();

		memory.read();
		demux.put(extbus1.get());// points to the correct register
		demuxRegisterInternalRead();

		incrementarPC();

		memory.read();
		demux.put(extbus1.get());

		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store();

		demuxRegisterRead(); // performs an internal store for the register identified into demux bus
		IR.store();
		IR.internalRead();

		if (intbus1.get() == intbus2.get()) {
			PC.read();
			memory.read();
			PC.store();
		} else {
			ula.inc();
			ula.internalRead(1);
			IR.internalStore();
			IR.read();
			PC.store();
		}
	}

	public void jgt() {
		incrementarPC();

		memory.read();
		demux.put(extbus1.get());// points to the correct register
		demuxRegisterInternalRead();

		incrementarPC();

		memory.read();
		demux.put(extbus1.get());

		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store();

		demuxRegisterRead(); // performs an internal store for the register identified into demux bus
		IR.store();
		IR.internalRead();

		if (intbus2.get() > intbus1.get()) {
			PC.read();
			memory.read();
			PC.store();
		} else {
			ula.inc();
			ula.internalRead(1);
			IR.internalStore();
			IR.read();
			PC.store();
		}
	}

	public void jlw() {
		incrementarPC();

		memory.read();
		demux.put(extbus1.get());// points to the correct register
		demuxRegisterInternalRead();

		incrementarPC();

		memory.read();
		demux.put(extbus1.get());

		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store();

		demuxRegisterRead(); // performs an internal store for the register identified into demux bus
		IR.store();
		IR.internalRead();

		if (intbus2.get() < intbus1.get()) {
			PC.read();
			memory.read();
			PC.store();
		} else {
			ula.inc();
			ula.internalRead(1);
			IR.internalStore();
			IR.read();
			PC.store();
		}
	}

	public void ldi() {
		incrementarPC();

		memory.read(); // the first register id is now in the external bus.
		demux.put(extbus1.get());

		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store();

		memory.read();
		demuxRegisterStore();

		incrementarPC();
	}

	public void read() { // read mem %RegA // RegA<-mem[mem]
		incrementarPC();

		memory.read(); // the address is now in the external bus.
		memory.read(); // the data is now in the external bus.
		demux.put(extbus1.get());

		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store();

		extbus1.put(demux.get());
		IR.store();

		PC.read();
		memory.read();
		demux.put(extbus1.get());

		IR.read();
		demuxRegisterStore();

		incrementarPC();
	}

	public void store() { // store %RegA mem // mem[mem]<- RegA
		incrementarPC();

		memory.read();
		demux.put(extbus1.get());

		ula.inc();
		ula.internalRead(1);
		IR.internalStore();
		IR.read();
		PC.store();

		memory.read();
		memory.store(); // the address is in the memory. Now we must to send the data
		demuxRegisterRead();
		memory.store(); // the data is now stored

		incrementarPC();
	}

	public void inc() {

		RPG.internalRead();
		ula.store(1);
		ula.inc();
		ula.read(1);
		setStatusFlags(intbus1.get());
		RPG.internalStore();

		incrementarPC();
	}

	public void add() {
		incrementarPC();

		RPG.internalRead();
		ula.store(0); // the rpg value is in ULA (0). This is the first parameter

		memory.read(); // the parameter is now in the external bus.
						// but the parameter is an address and we need the value
		memory.read(); // now the value is in the external bus
		RPG.store();
		RPG.internalRead();
		ula.store(1); // the rpg value is in ULA (0). This is the second parameter
		ula.add(); // the result is in the second ula's internal register
		ula.read(1);
		; // the operation result is in the internalbus 2
		setStatusFlags(intbus1.get()); // changing flags due the end of the operation
		RPG.internalStore(); // now the add is complete

		incrementarPC();
	}

	public void sub() {
		incrementarPC();

		RPG.internalRead();
		ula.store(0); // the rpg value is in ULA (0). This is the first parameter

		memory.read(); // the parameter is now in the external bus.
						// but the parameter is an address and we need the value
		memory.read(); // now the value is in the external bus
		RPG.store();
		RPG.internalRead();
		ula.store(1); // the rpg value is in ULA (0). This is the second parameter
		ula.sub(); // the result is in the second ula's internal register
		ula.read(1);
		; // the operation result is in the internalbus 2
		setStatusFlags(intbus1.get()); // changing flags due the end of the operation
		RPG.internalStore(); // now the sub is complete

		incrementarPC();
	}

	public ArrayList<Register> getRegistersList() {
		return registersList;
	}

	/**
	 * This method performs an (external) read from a register into the register
	 * list.
	 * The register id must be in the demux bus
	 */
	private void demuxRegisterRead() {
		registersList.get(demux.get()).read();
	}

	/**
	 * This method performs an (internal) read from a register into the register
	 * list.
	 * The register id must be in the demux bus
	 */
	private void demuxRegisterInternalRead() {
		registersList.get(demux.get()).internalRead();
		;
	}

	/**
	 * This method performs an (external) store toa register into the register list.
	 * The register id must be in the demux bus
	 */
	private void demuxRegisterStore() {
		registersList.get(demux.get()).store();
	}

	/**
	 * This method performs an (internal) store toa register into the register list.
	 * The register id must be in the demux bus
	 */
	private void demuxRegisterInternalStore() {
		registersList.get(demux.get()).internalStore();
		;
	}

	/**
	 * This method reads an entire file in machine code and
	 * stores it into the memory
	 * NOT TESTED
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public void readExec(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename + ".dxf"));
		String linha;
		int i = 0;
		while ((linha = br.readLine()) != null) {
			extbus1.put(i);
			memory.store();
			extbus1.put(Integer.parseInt(linha));
			memory.store();
			i++;
		}
		br.close();
	}

	/**
	 * This method executes a program that is stored in the memory
	 */
	public void controlUnitEexec() {
		halt = false;
		while (!halt) {
			fetch();
			decodeExecute();
		}

	}

	/**
	 * This method implements The decode proccess,
	 * that is to find the correct operation do be executed
	 * according the command.
	 * And the execute proccess, that is the execution itself of the command
	 */
	private void decodeExecute() {
		IR.internalRead(); // the instruction is in the internalbus2
		int command = intbus2.get();
		simulationDecodeExecuteBefore(command);
		switch (command) {
			case 0:
				addRegReg();
				break;
			case 1:
				addMemReg();
				break;
			case 2:
				addRegMem();
				break;
			case 3:
				subRegReg();
				break;
			case 4:
				subMemReg();
				break;
			case 5:
				subRegMem();
				break;
			case 6:
				imulMemReg();
				break;
			case 7:
				imulRegMem();
				break;
			case 8:
				imulRegReg();
				break;
			case 9:
				moveMemReg();
				break;
			case 10:
				moveRegMem();
				break;
			case 11:
				moveRegReg();
				break;
			case 12:
				moveImmReg();
				break;
			case 13:
				incReg();
				break;
			case 14:
				incMem();
				break;
			case 15:
				jmp();
				break;
			case 16:
				jn();
				break;
			case 17:
				jz();
				break;
			case 18:
				jnz();
				break;
			case 19:
				jeq();
				break;
			case 20:
				jgt();
				break;
			case 21:
				jlw();
				break;
			case 22:
				ldi();
				break;
			case 23:
				read();
				break;
			case 24:
				store();
				break;
			default:
				halt = true;
				break;
		}
		if (simulation)
			simulationDecodeExecuteAfter();
	}

	/**
	 * This method is used to show the components status in simulation conditions
	 * NOT TESTED
	 * 
	 * @param command
	 */
	private void simulationDecodeExecuteBefore(int command) {
		System.out.println("----------BEFORE Decode and Execute phases--------------");
		String instruction;
		int parameter = 0;
		for (Register r : registersList) {
			System.out.println(r.getRegisterName() + ": " + r.getData());
		}
		if (command != -1)
			instruction = commandsList.get(command);
		else
			instruction = "END";
		if (hasOperands(instruction)) {
			parameter = memory.getDataList()[PC.getData() + 1];
			System.out.println("Instruction: " + instruction + " " + parameter);
		} else
			System.out.println("Instruction: " + instruction);
		if ("read".equals(instruction))
			System.out.println("memory[" + parameter + "]=" + memory.getDataList()[parameter]);

	}

	/**
	 * This method is used to show the components status in simulation conditions
	 * NOT TESTED
	 */
	private void simulationDecodeExecuteAfter() {
		String instruction;
		System.out.println("-----------AFTER Decode and Execute phases--------------");
		System.out.println("Internal Bus 1: " + intbus1.get());
		System.out.println("Internal Bus 2: " + intbus2.get());
		System.out.println("External Bus 1: " + extbus1.get());
		for (Register r : registersList) {
			System.out.println(r.getRegisterName() + ": " + r.getData());
		}
		Scanner entrada = new Scanner(System.in);
		System.out.println("Press <Enter>");
		String mensagem = entrada.nextLine();
	}

	/**
	 * This method uses PC to find, in the memory,
	 * the command code that must be executed.
	 * This command must be stored in IR
	 * NOT TESTED!
	 */
	private void fetch() {
		PC.read();
		memory.read();
		IR.store();
		simulationFetch();
	}

	/**
	 * This method is used to show the components status in simulation conditions
	 * NOT TESTED!!!!!!!!!
	 */
	private void simulationFetch() {
		if (simulation) {
			System.out.println("-------Fetch Phase------");
			System.out.println("PC: " + PC.getData());
			System.out.println("IR: " + IR.getData());
		}
	}

	/**
	 * This method is used to show in a correct way the operands (if there is any)
	 * of instruction,
	 * when in simulation mode
	 * NOT TESTED!!!!!
	 * 
	 * @param instruction
	 * @return
	 */
	private boolean hasOperands(String instruction) {
		if ("inc".equals(instruction)) // inc is the only one instruction having no operands
			return false;
		else
			return true;
	}

	/**
	 * This method returns the amount of positions allowed in the memory
	 * of this architecture
	 * NOT TESTED!!!!!!!
	 * 
	 * @return
	 */
	public int getMemorySize() {
		return memorySize;
	}

	public static void main(String[] args) throws IOException {
		Assembler.main(null);
		Architecture arch = new Architecture(true);
		arch.readExec("program");
		arch.controlUnitEexec();
	}

}
