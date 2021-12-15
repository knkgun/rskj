package co.rsk.peg.resources;

import co.rsk.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;

public class TestConstants {
    public static final Script ERP_TESTNET_REDEEM_SCRIPT = new Script(Hex.decode("64522102a7ff948f7bf0f1d2f6f425922490e20adf32bd6c5f75f4b4364cacdc7f4cbf8e2102b207b40ec4a7d86bf4abaa2461b57ee408d689d0ce21851c5fd6ece05ef35c0421034cc7cbe4c1dfcd0f32385a8a2b1de4b79098cd3429b8944b16157af78b2b38ec53670300cd50b27552210216c23b2ea8e4f11c3f9e22711addb1d16a93964796913830856b568cc3ea21d3210275562901dd8faae20de0a4166362a4f82188db77dbed4ca887422ea1ec185f1421034db69f2112f4fb1bb6141bf6e2bd6631f0484d0bd95b16767902c9fe219d4a6f5368ae"));
}
