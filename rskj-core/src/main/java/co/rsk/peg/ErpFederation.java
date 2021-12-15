package co.rsk.peg;

import co.rsk.bitcoinj.core.Address;
import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.bitcoinj.script.ErpFederationRedeemScriptParser;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import co.rsk.peg.utils.EcKeyUtils;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.bouncycastle.util.encoders.Hex;
import org.ethereum.config.blockchain.upgrades.ActivationConfig;
import org.ethereum.config.blockchain.upgrades.ConsensusRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErpFederation extends Federation {
    private static final Logger logger = LoggerFactory.getLogger(ErpFederation.class);

    private final List<BtcECKey> erpPubKeys;
    private final long activationDelay;
    private final ActivationConfig.ForBlock activations;

    private Script standardRedeemScript;
    private Script standardP2SHScript;

    public ErpFederation(
        List<FederationMember> members,
        Instant creationTime,
        long creationBlockNumber,
        NetworkParameters btcParams,
        List<BtcECKey> erpPubKeys,
        long activationDelay,
        ActivationConfig.ForBlock activations
    ) {
        super(members, creationTime, creationBlockNumber, btcParams);
        this.erpPubKeys = EcKeyUtils.getCompressedPubKeysList(erpPubKeys);
        this.activationDelay = activationDelay;
        this.activations = activations;

        // Try getting the redeem script in order to validate it can be built
        // using the given public keys and csv value
        getRedeemScript();
    }

    public List<BtcECKey> getErpPubKeys() {
        return Collections.unmodifiableList(erpPubKeys);
    }

    public long getActivationDelay() {
        return activationDelay;
    }

    @Override
    public final Script getRedeemScript() {
        if (!activations.isActive(ConsensusRule.RSKIP284) &&
            btcParams.getId().equals(NetworkParameters.ID_TESTNET)) {
            logger.debug("[getRedeemScript] Returning hardcoded redeem script");
            final byte[] ERP_TESTNET_REDEEM_SCRIPT_BYTES = Hex.decode("64522102a7ff948f7bf0f1d2f6f425922490e20adf32bd6c5f75f4b4364cacdc7f4cbf8e2102b207b40ec4a7d86bf4abaa2461b57ee408d689d0ce21851c5fd6ece05ef35c0421034cc7cbe4c1dfcd0f32385a8a2b1de4b79098cd3429b8944b16157af78b2b38ec53670300cd50b27552210216c23b2ea8e4f11c3f9e22711addb1d16a93964796913830856b568cc3ea21d3210275562901dd8faae20de0a4166362a4f82188db77dbed4ca887422ea1ec185f1421034db69f2112f4fb1bb6141bf6e2bd6631f0484d0bd95b16767902c9fe219d4a6f5368ae");
            return new Script(ERP_TESTNET_REDEEM_SCRIPT_BYTES);
        }

        if (redeemScript == null) {
            logger.debug("[getRedeemScript] Creating the redeem script from the keys");
            redeemScript = ErpFederationRedeemScriptParser.createErpRedeemScript(
                ScriptBuilder.createRedeemScript(getNumberOfSignaturesRequired(), getBtcPublicKeys()),
                ScriptBuilder.createRedeemScript(erpPubKeys.size() / 2 + 1, erpPubKeys),
                activationDelay
            );
        }

        return redeemScript;
    }

    @Override
    public Script getStandardRedeemScript() {
        if (standardRedeemScript == null) {
            standardRedeemScript = ErpFederationRedeemScriptParser.extractStandardRedeemScript(
                getRedeemScript().getChunks()
            );
        }
        return standardRedeemScript;
    }

    @Override
    public Script getP2SHScript() {
        if (p2shScript == null) {
            p2shScript = ScriptBuilder.createP2SHOutputScript(getRedeemScript());
        }

        return p2shScript;
    }

    @Override
    public Script getStandardP2SHScript() {
        if (standardP2SHScript == null) {
            standardP2SHScript = ScriptBuilder.createP2SHOutputScript(getStandardRedeemScript());
        }

        return standardP2SHScript;
    }

    @Override
    public Address getAddress() {
        if (address == null) {
            address = Address.fromP2SHScript(btcParams, getP2SHScript());
        }

        return address;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }

        ErpFederation otherErpFederation = (ErpFederation) other;

        return this.getNumberOfSignaturesRequired() == otherErpFederation.getNumberOfSignaturesRequired() &&
            this.getSize() == otherErpFederation.getSize() &&
            this.getCreationTime().equals(otherErpFederation.getCreationTime()) &&
            this.creationBlockNumber == otherErpFederation.creationBlockNumber &&
            this.btcParams.equals(otherErpFederation.btcParams) &&
            this.members.equals(otherErpFederation.members) &&
            this.getRedeemScript().equals(otherErpFederation.getRedeemScript()) &&
            this.erpPubKeys.equals(otherErpFederation.erpPubKeys) &&
            this.activationDelay == otherErpFederation.activationDelay;
    }

    @Override
    public int hashCode() {
        // Can use java.util.Objects.hash since all of Instant, int and List<BtcECKey> have
        // well-defined hashCode()s
        return Objects.hash(
            getCreationTime(),
            this.creationBlockNumber,
            getNumberOfSignaturesRequired(),
            getBtcPublicKeys(),
            getErpPubKeys(),
            getActivationDelay()
        );
    }
}
