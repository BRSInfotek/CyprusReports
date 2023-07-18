package org.cyprusbrs.process;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.cyprusbrs.model.MDocType;
import org.cyprusbrs.model.MInvoice;
import org.cyprusbrs.model.MPayment;
import org.cyprusbrs.util.CyprusSystemError;
import org.cyprusbrs.util.Env;

public class GeneratePaymentFromInvoice extends SvrProcess {

	
	private Integer p_bankAccounId=0;
	@Override
	protected void prepare() {
		
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("C_BankAccount_ID"))
				p_bankAccounId = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}

	@Override
	protected String doIt() throws Exception {
		int Table_ID = getTable_ID();
		int Record_ID = getRecord_ID();
		log.info ("Table_ID=" + Table_ID + ", Record_ID=" + Record_ID);
		
		String retValue=createPayment(new MInvoice(getCtx(), Record_ID, get_TrxName()));
		if(retValue!=null)
			return retValue;
		return "Payment is not created";
	}

	/**
	 * 
	 * @param mInvoice
	 * @return
	 */
	private String createPayment(MInvoice mInvoice)throws Exception {
		
		//
		
		Integer docType=0;
		Boolean checkReciept=false;
		if(mInvoice.isSOTrx())
		{
			docType=MDocType.getDocType(MDocType.DOCBASETYPE_ARReceipt);
			checkReciept=true;
		}
		else
			docType=MDocType.getDocType(MDocType.DOCBASETYPE_APPayment);
		
		MPayment payment = createPayment (mInvoice.getC_Invoice_ID(), mInvoice.getC_BPartner_ID(),
				mInvoice.getC_Currency_ID(), mInvoice.getGrandTotal(), mInvoice.getGrandTotal(), 
				p_bankAccounId, mInvoice.getDateInvoiced(), mInvoice.getDateAcct(),
				mInvoice.getDescription(), mInvoice.getAD_Org_ID(),docType,checkReciept);
		if (payment == null)
			throw new CyprusSystemError("Could not create Payment");
		
		String retString = "@C_Payment_ID@ = " + payment.getDocumentNo();
		if (payment.getOverUnderAmt().signum() != 0)
			retString += " - @OverUnderAmt@=" + payment.getOverUnderAmt();
		return retString;
	}

	/**
	 * 
	 * @param C_Invoice_ID
	 * @param C_BPartner_ID
	 * @param C_Currency_ID
	 * @param StmtAmt
	 * @param TrxAmt
	 * @param C_BankAccount_ID
	 * @param DateTrx
	 * @param DateAcct
	 * @param Description
	 * @param AD_Org_ID
	 * @param docType 
	 * @param checkReciept 
	 * @return
	 */
	private MPayment createPayment (int C_Invoice_ID, int C_BPartner_ID, 
			int C_Currency_ID, BigDecimal StmtAmt, BigDecimal TrxAmt,
			int C_BankAccount_ID, Timestamp DateTrx, Timestamp DateAcct, 
			String Description, int AD_Org_ID, Integer docType, Boolean checkReciept)
		{
			//	Trx Amount = Payment overwrites Statement Amount if defined
			BigDecimal PayAmt = TrxAmt;
			if (PayAmt == null || Env.ZERO.compareTo(PayAmt) == 0)
				PayAmt = StmtAmt;
			if (C_Invoice_ID == 0
				&& (PayAmt == null || Env.ZERO.compareTo(PayAmt) == 0))
				throw new IllegalStateException ("@PayAmt@ = 0");
			if (PayAmt == null)
				PayAmt = Env.ZERO;
			//
			MPayment payment = new MPayment (getCtx(), 0, get_TrxName());
			payment.setC_DocType_ID(docType);
			payment.setIsReceipt(checkReciept);
			payment.setC_BankAccount_ID(p_bankAccounId);
			payment.setAD_Org_ID(AD_Org_ID);
			payment.setC_BankAccount_ID(C_BankAccount_ID);
			payment.setTenderType(MPayment.TENDERTYPE_Account);
			if (DateTrx != null)
				payment.setDateTrx(DateTrx);
			else if (DateAcct != null)
				payment.setDateTrx(DateAcct);
			if (DateAcct != null)
				payment.setDateAcct(DateAcct);
			else
				payment.setDateAcct(payment.getDateTrx());
			payment.setDescription(Description);
			//
			if (C_Invoice_ID != 0)
			{
				MInvoice invoice = new MInvoice (getCtx(), C_Invoice_ID, null);
				payment.setC_DocType_ID(invoice.isSOTrx());		//	Receipt
				payment.setC_Invoice_ID(invoice.getC_Invoice_ID());
				payment.setC_BPartner_ID (invoice.getC_BPartner_ID());
				if (PayAmt.signum() != 0)	//	explicit Amount
				{
					payment.setC_Currency_ID(C_Currency_ID);
					if (invoice.isSOTrx())
						payment.setPayAmt(PayAmt);
					else	//	payment is likely to be negative
						payment.setPayAmt(PayAmt.negate());
					
					/// if required then activate over-under payment
//					payment.setOverUnderAmt(invoice.getGrandTotal(true).subtract(payment.getPayAmt()));
				}
				else	// set Pay Amout from Invoice
				{
					payment.setC_Currency_ID(invoice.getC_Currency_ID());
					payment.setPayAmt(invoice.getGrandTotal(true));
				}
			}
			else if (C_BPartner_ID != 0)
			{
				payment.setC_BPartner_ID(C_BPartner_ID);
				payment.setC_Currency_ID(C_Currency_ID);
				if (PayAmt.signum() < 0)	//	Payment
				{
					payment.setPayAmt(PayAmt.abs());
					payment.setC_DocType_ID(false);
				}
				else	//	Receipt
				{
					payment.setPayAmt(PayAmt);
					payment.setC_DocType_ID(true);
				}
			}
			else
				return null;
			payment.save();
			// We are completing the payment 
//			payment.processIt(MPayment.DOCACTION_Complete);
//			payment.save();
			return payment;		
		}	//	createPayment
}
