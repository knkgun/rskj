/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.core.bc; 

import co.rsk.blockchain.utils.BlockGenerator;
import co.rsk.config.TestSystemProperties;
import co.rsk.config.VmConfig;
import co.rsk.core.Coin;
import co.rsk.core.RskAddress;
import co.rsk.core.TransactionExecutorFactory;
import co.rsk.db.MutableTrieImpl;
import co.rsk.db.RepositoryLocator;
import co.rsk.db.RepositorySnapshot;
import co.rsk.db.StateRootHandler;
import co.rsk.peg.BridgeSupportFactory;
import co.rsk.peg.BtcBlockStoreWithCache.Factory;
import co.rsk.peg.RepositoryBtcBlockStoreWithCache;
import co.rsk.trie.Trie;
import co.rsk.trie.TrieConverter;
import co.rsk.trie.TrieStore;
import co.rsk.trie.TrieStoreImpl;
import co.rsk.vm.BytecodeCompiler;

import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Hex;

import org.ethereum.config.blockchain.upgrades.ActivationConfig;
import org.ethereum.config.blockchain.upgrades.ConsensusRule;
import org.ethereum.core.*;
import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;
import org.ethereum.crypto.cryptohash.Keccak256;
import org.ethereum.datasource.HashMapDB;
import org.ethereum.db.MutableRepository;
import org.ethereum.listener.TestCompositeEthereumListener;
import org.ethereum.net.eth.message.StatusMessage;
import org.ethereum.net.message.Message;
import org.ethereum.net.p2p.HelloMessage;
import org.ethereum.net.rlpx.Node;
import org.ethereum.net.server.Channel;
//import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RskTestFactory;
import org.ethereum.vm.PrecompiledContracts;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.VM;
import org.ethereum.vm.program.Program;
import org.ethereum.vm.program.Stack;
import org.ethereum.vm.program.invoke.ProgramInvoke;
import org.ethereum.vm.program.invoke.ProgramInvokeMockImpl;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;

import static org.ethereum.config.blockchain.upgrades.ConsensusRule.RSKIP125;
import static org.mockito.Mockito.*;

/**
 * Derived from BlockExecutorTest class Created by ajlopez on 29/07/2016.
 * by smishra June 2020 for storage rent
 * Even the simplest block exec tests have to be modified:
     * We use a single gaslimit field in TX which is split 50:50 between execution gas and rent gas. 
        Thus, gasLimit in the example/tests transaction has to be at least doubled from 21K to 42K+
    * The increase in gaslimit, implies sender balances have to be increased accordingly as well
    * Several assertion tests rely on the predictability of execution gas, gas refunds, and balance changes.
      - These no longer work as rent gas relies on difference in timestamps (less predictable) 
      - Furthermore RSKIP113 has collection thresholds intended to avoid small transactions (too many disk writes).
 */
public class BlockExecRentTest {
    private ActivationConfig.ForBlock activationConfig;
    private BytecodeCompiler compiler = new BytecodeCompiler();
    private ProgramInvokeMockImpl invoke = new ProgramInvokeMockImpl();
    private final VmConfig vmConfig = config.getVmConfig();
    private final PrecompiledContracts precompiledContracts = new PrecompiledContracts(
            config,
            new BridgeSupportFactory(
                    new RepositoryBtcBlockStoreWithCache.Factory(
                            config.getNetworkConstants().getBridgeConstants().getBtcParams()),
                    config.getNetworkConstants().getBridgeConstants(),
                    config.getActivationConfig()));    

    public static final byte[] EMPTY_TRIE_HASH = sha3(RLP.encodeElement(EMPTY_BYTE_ARRAY));
    private static final TestSystemProperties config = new TestSystemProperties();
    private static final BlockFactory blockFactory = new BlockFactory(config.getActivationConfig());

    private Blockchain blockchain;
    private BlockExecutor executor;
    private TrieStore trieStore;
     //#mish recall block executor forks the repository and uses a snapshot
    private RepositorySnapshot repository;

    @Before
    public void setUp() {
        RskTestFactory objects = new RskTestFactory(config); //#mish todo: this has been deprecated! use RSKtestcontext
        blockchain = objects.getBlockchain();
        executor = objects.getBlockExecutor();
        trieStore = objects.getTrieStore();
        repository = objects.getRepositoryLocator().snapshotAt(blockchain.getBestBlock().getHeader());
        
        activationConfig = mock(ActivationConfig.ForBlock.class);
        when(activationConfig.isActive(RSKIP125)).thenReturn(true);
    }

    @Test
    public void executeBlockWithOneTransaction() {
        Block block = getBlockWithOneTransaction(); // this changes the best block
        Block parent = blockchain.getBestBlock();


        Transaction tx = block.getTransactionsList().get(0);
        RskAddress account = tx.getSender();
        
        System.out.println("\nSender Bal " + repository.getBalance(account));
        
        BlockResult result = executor.execute(block, parent.getHeader(), false);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getTransactionReceipts()); 
        Assert.assertFalse(result.getTransactionReceipts().isEmpty());
        Assert.assertEquals(1, result.getTransactionReceipts().size());

        TransactionReceipt receipt = result.getTransactionReceipts().get(0);
        Assert.assertEquals(tx, receipt.getTransaction());
        
        //AccountState accountState = repository.getAccountState(account);
            
        Repository finalRepository = new MutableRepository(trieStore,
                trieStore.retrieve(result.getFinalState().getHash().getBytes()).get());

        AccountState accountState = finalRepository.getAccountState(account);
        System.out.println("Sender Bal " + accountState.getBalance());
        System.out.println("Sender LRPT " + finalRepository.getAccountNodeLRPTime(account));

    }

    //a send but TX also has data. Also used to compare results with RPC estimateGas (curl examples et end of file) 
    @Test
    public void executeBlockWithOneDataTransaction() {
        Block block = getBlockWithOneDataTransaction(); // this changes the best block
        Block parent = blockchain.getBestBlock();

        Transaction tx = block.getTransactionsList().get(0);
        RskAddress account = tx.getSender();
        
        System.out.println("\nSender Bal " + repository.getBalance(account));
        
        BlockResult result = executor.execute(block, parent.getHeader(), false);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getTransactionReceipts()); 
        Assert.assertFalse(result.getTransactionReceipts().isEmpty());
        Assert.assertEquals(1, result.getTransactionReceipts().size());

        TransactionReceipt receipt = result.getTransactionReceipts().get(0);
        Assert.assertEquals(tx, receipt.getTransaction());
        
        //AccountState accountState = repository.getAccountState(account);
            
        Repository finalRepository = new MutableRepository(trieStore,
                trieStore.retrieve(result.getFinalState().getHash().getBytes()).get());

        AccountState accountState = finalRepository.getAccountState(account);
        System.out.println("Sender Bal " + accountState.getBalance());
        System.out.println("Sender LRPT " + finalRepository.getAccountNodeLRPTime(account));

    }
        
    @Test
    public void executeBlockWithOneCreateTransaction() {
        //trying a different version
        Block block = getBlockWithOneCreateTransaction(); // this changes the best block
        Block parent = blockchain.getBestBlock();

        Transaction tx = block.getTransactionsList().get(0);
        RskAddress account = tx.getSender();
        RskAddress contractAddr = tx.getContractAddress();
        System.out.println("\nSender: " + account);
        System.out.println("Contract: " + contractAddr);
        System.out.println("TX Data: 0x" + Hex.toHexString(tx.getData()));
        System.out.println("TX value: " + tx.getValue());

        when(activationConfig.isActive(RSKIP125)).thenReturn(false);
        BlockResult result = executor.execute(block, parent.getHeader(), false);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getTransactionReceipts()); 
        Assert.assertFalse(result.getTransactionReceipts().isEmpty());
        Assert.assertEquals(1, result.getTransactionReceipts().size());

        TransactionReceipt receipt = result.getTransactionReceipts().get(0);
        Assert.assertEquals(tx, receipt.getTransaction());


        Repository finalRepository = new MutableRepository(trieStore,
                trieStore.retrieve(result.getFinalState().getHash().getBytes()).get());

        AccountState accountState = finalRepository.getAccountState(account);
        System.out.println("Sender Bal " + accountState.getBalance());
        System.out.println("Sender LRPT " + finalRepository.getAccountNodeLRPTime(account));

        // Same for contract (check endowment)
        AccountState contractState = finalRepository.getAccountState(contractAddr);
        System.out.println("Contract Endowment " + contractState.getBalance());
        System.out.println("Contract LRPT " + finalRepository.getAccountNodeLRPTime(contractAddr));

        System.out.println("\nBlock tx fees: " + result.getPaidFees()); 


        // After execution, just here to experiment with diff programs
        /* //based on CREATE test in CREATE2 test of rsk.vm (Seba's)
        System.out.println("\nDirect program run\n"); 
        String code = "PUSH1 0x01 PUSH1 0x02 PUSH1 0x00 CREATE";

        Program program = executeTxCode(code, tx);

        Stack stack = program.getStack();
        String address = Hex.toHexString(stack.peek().getLast20Bytes());
        System.out.println("\ncontract addr " + address);
        long nonce = program.getStorage().getNonce(new RskAddress(address)).longValue();
        System.out.println("\nrec addr " + tx.getReceiveAddress());

        Assert.assertEquals(0, nonce);
        Assert.assertEquals("77045E71A7A2C50903D88E564CD72FAB11E82051", address.toUpperCase());
        Assert.assertEquals(1, stack.size());
        */
    }
    
    private Block getBlockWithOneTransaction() {
        // first we modify the best block to have two accounts with balance
        Repository track = repository.startTracking();

         // #mish #increased sender balance from 30_000 to 44K (cover both gas fees) + BigInteger.TEN for tansfer TX value
        //e94aef644e428941ee0a3741f28d80255fddba7f
        Account account = createAccount("acctest1", track, Coin.valueOf(44010));
        Account account2 = createAccount("acctest2", track, Coin.valueOf(10L));

        track.commit();

        Block bestBlock = blockchain.getBestBlock();
        bestBlock.setStateRoot(repository.getRoot());

        // then we create the new block to connect
        List<Transaction> txs = Collections.singletonList(
                createTransaction(account, account2, BigInteger.TEN, repository.getNonce(account.getAddress()))
        );

        List<BlockHeader> uncles = new ArrayList<>();
                    //many signatures, this one is createChildBlock(parentBlock,TxList, UncleList, difficulty, mingasprice)
        return new BlockGenerator().createChildBlock(bestBlock, txs, uncles, 1, null);
    }

    private Block getBlockWithOneDataTransaction() {
        // first we modify the best block to have two accounts with balance
        Repository track = repository.startTracking();

        Account account = createAccount("acctest1", track, Coin.valueOf(200010));
        Account account2 = createAccount("acctest2", track, Coin.valueOf(10L));
        String txData = "d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675";         
        track.commit();

        Block bestBlock = blockchain.getBestBlock();
        bestBlock.setStateRoot(repository.getRoot());

        // then we create the new block to connect
        List<Transaction> txs = Collections.singletonList(
                createTxWithData(account, account2, BigInteger.TEN, repository.getNonce(account.getAddress()), txData)
        );

        List<BlockHeader> uncles = new ArrayList<>();
                    //many signatures, this one is createChildBlock(parentBlock,TxList, UncleList, difficulty, mingasprice)
        return new BlockGenerator().createChildBlock(bestBlock, txs, uncles, 1, null);
    }
    

    private Block getBlockWithOneCreateTransaction() {
        // first we modify the best block to have two accounts with balance
        Repository track = repository.startTracking();

        Account account = createAccount("acctest1", track, Coin.valueOf(2000010)); //#mish create needs 53K, plus 1/2 for rent

        // This example wastes gas. The tx data has a create opcode. Will consume 21K + 32K (empty receive => create) + 32k for CREATE opcode
        String stringCode = "PUSH1 0x01 PUSH1 0x02 PUSH1 0x00 PUSH1 0x01 PUSH1 0x02 PUSH1 0x00";// CREATE"; // CREATE needs 3 args value start size
        byte[] code = compiler.compile(stringCode);
        String codeHex = Hex.toHexString(code);

        //System.out.println("\nProgram code in TX Data: 0x" + codeHex);
        track.commit();

        Block bestBlock = blockchain.getBestBlock();
        bestBlock.setStateRoot(repository.getRoot());

        // then we create the new block to connect
        List<Transaction> txs = Collections.singletonList(
                createTxNullAddr(account, BigInteger.TEN, repository.getNonce(account.getAddress()), codeHex)
        );

        List<BlockHeader> uncles = new ArrayList<>();
        return new BlockGenerator().createChildBlock(bestBlock, txs, uncles, 1, null);
    }

    private static Transaction createTransaction(Account sender, Account receiver, BigInteger value, BigInteger nonce) {
        String toAddress = Hex.toHexString(receiver.getAddress().getBytes());
        byte[] privateKeyBytes = sender.getEcKey().getPrivKeyBytes();
        
        /* #mish:
         * string to, BigInteger amount, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, byte chainId
         * The gaslimit (orig 21K) has to be increased to at least 42K, so that 1/2 of that can cover basic execution gas.
         * sender's balance needs to be updated as well 
        */
        Transaction tx = new Transaction(toAddress, value, nonce, BigInteger.ONE, BigInteger.valueOf(44000), config.getNetworkConstants().getChainId());
        tx.sign(privateKeyBytes);
        return tx;
    }

    private static Transaction createTxWithData(Account sender, Account receiver, BigInteger value, BigInteger nonce, String data) {
        String toAddress = Hex.toHexString(receiver.getAddress().getBytes());
        byte[] privateKeyBytes = sender.getEcKey().getPrivKeyBytes();
        //e.g. data: "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675"
        // this example from web search on using eth_estimategas
        Transaction tx = new Transaction(toAddress, value, nonce, BigInteger.ONE, BigInteger.valueOf(90000), data, config.getNetworkConstants().getChainId());
        tx.sign(privateKeyBytes);
        return tx;
    }

    // #mish with no receiver (NULL for contract creation TX) data in arglist and
    private static Transaction createTxNullAddr(Account sender, BigInteger value, BigInteger nonce, String data) {
        byte[] privateKeyBytes = sender.getEcKey().getPrivKeyBytes();
        
        Transaction tx = new Transaction(null, value, nonce, BigInteger.ONE, BigInteger.valueOf(200000), data, config.getNetworkConstants().getChainId());
        tx.sign(privateKeyBytes);
        return tx;
    }

    public static Account createAccount(String seed, Repository repository, Coin balance) {
        Account account = createAccount(seed);
        repository.createAccount(account.getAddress());
        repository.addBalance(account.getAddress(), balance);
        return account;
    }

    public static Account createAccount(String seed) {
        byte[] privateKeyBytes = HashUtil.keccak256(seed.getBytes());
        ECKey key = ECKey.fromPrivate(privateKeyBytes);
        Account account = new Account(key);
        return account;
    }



    private byte[] calculateTxTrieRoot(List<Transaction> transactions, long blockNumber) {
        return BlockHashesHelper.getTxTrieRoot(
                transactions,
                config.getActivationConfig().isActive(ConsensusRule.RSKIP126, blockNumber)
        );
    }

    private static byte[] sha3(byte[] input) {
        Keccak256 digest = new Keccak256();
        digest.update(input);
        return digest.digest();
    }

    private static BlockExecutor buildBlockExecutor(TrieStore store) {
        StateRootHandler stateRootHandler = new StateRootHandler(
                config.getActivationConfig(), new TrieConverter(), new HashMapDB(), new HashMap<>());

        Factory btcBlockStoreFactory = new RepositoryBtcBlockStoreWithCache.Factory(
                config.getNetworkConstants().getBridgeConstants().getBtcParams());

        BridgeSupportFactory bridgeSupportFactory = new BridgeSupportFactory(
                btcBlockStoreFactory, config.getNetworkConstants().getBridgeConstants(), config.getActivationConfig());

        return new BlockExecutor(
                config.getActivationConfig(),
                new RepositoryLocator(store, stateRootHandler),
                stateRootHandler,
                new TransactionExecutorFactory(
                        config,
                        null,
                        null,
                        blockFactory,
                        new ProgramInvokeFactoryImpl(),
                        new PrecompiledContracts(config, bridgeSupportFactory),
                        new BlockTxSignatureCache(new ReceivedTxSignatureCache())
                )
        );
    }

    public static class TestObjects {

        private TrieStore trieStore;
        private Block block;
        private Block parent;
        private Transaction transaction;
        private Account account;
        byte[] rootPriorExecution;


        public TestObjects(TrieStore trieStore, Block block, Block parent, Transaction transaction, Account account) {
            this.trieStore = trieStore;
            this.block = block;
            this.parent = parent;
            this.transaction = transaction;
            this.account = account;
        }

        public TestObjects(
                TrieStore trieStore,
                Block block,
                Block parent,
                Transaction transaction,
                Account account,
                byte[] rootPriorExecution) {
            this.trieStore = trieStore;
            this.block = block;
            this.parent = parent;
            this.transaction = transaction;
            this.account = account;
            this.rootPriorExecution = rootPriorExecution;
        }

        public TrieStore getTrieStore() {
            return this.trieStore;
        }

        public Block getBlock() {
            return this.block;
        }

        public Block getParent() {
            return this.parent;
        }

        public Transaction getTransaction() {
            return this.transaction;
        }

        public Account getAccount() {
            return this.account;
        }
    }

    // #mish helpers for Call/create TX
    private Program executeTxCode(String stringCode, Transaction tx) {
        byte[] code = compiler.compile(stringCode);
        VM vm = new VM(vmConfig,precompiledContracts);
        
        Program program = new Program(vmConfig, precompiledContracts, blockFactory, activationConfig, code, invoke, tx, new HashSet<>());

        while (!program.isStopped()){
            vm.step(program);
        }

        return program;
    }

    /** send with data
     curl localhost:4444
     -X POST -H "Content-Type: application/json"
     --data '{"jsonrpc":"2.0","method":"eth_estimateGas",
              "params": 
               [{"from": "e56e8dd67c5d32be8058bb8eb970870f07244567",
                "to": "0xd46e8dd67c5d32be8058bb8eb970870f07244567",
                "gas": "0xe000","gasPrice": "0x01","value": "0x9184e72a",
                "data": "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675"}],
             "id":1}'
     
     */ 

     /** create
     curl localhost:4444 
     -X POST -H "Content-Type: application/json" 
     --data '{"jsonrpc":"2.0","method":"eth_estimateGas",
              "params": 
               [{"from": "e56e8dd67c5d32be8058bb8eb970870f07244567",
                "to": "d46e8dd67c5d32be8058bb8eb970870f07244567",
                "gas": "0x30d40","gasPrice": "0x01","value": "0x9184e72a",
                "data": "0x600160026000f"}],
             "id":1}'
     
     One line: 
     curl localhost:4444 -X POST -H "Content-Type: application/json" --data '{"jsonrpc":"2.0","method":"eth_estimateGas","params": [{"from": "e56e8dd67c5d32be8058bb8eb970870f07244567","to": "","gas": "0x30d40","gasPrice": "0x9184e72a000","value": "0x9184e72a","data": "0x600160026000f"}],"id":1}'
     
      */




}