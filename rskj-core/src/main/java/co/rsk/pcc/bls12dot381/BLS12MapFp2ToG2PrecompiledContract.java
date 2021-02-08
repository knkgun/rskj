
package co.rsk.pcc.bls12dot381;

import co.rsk.bls12_381.BLS12_381;

public class BLS12MapFp2ToG2PrecompiledContract extends AbstractBLS12PrecompiledContract {

  @Override
  protected byte[] internalExecute(byte[] input) {
    return BLS12_381.mapFp2ToG2(input);
  }

  @Override
  public long getGasForData(byte[] data) {
    return 110_000;
  }
}