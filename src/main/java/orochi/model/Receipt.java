package orochi.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "Receipt")
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReceiptID")
    private Integer receiptId;

    @Column(name = "TransactionID")
    private Integer transactionId;

    @Column(name = "ReceiptNumber")
    private String receiptNumber;

    @Column(name = "IssuedDate")
    private LocalDate issuedDate;

    @Column(name = "Format")
    private String format;

    @Column(name = "TaxAmount")
    private BigDecimal taxAmount;

    @Column(name = "DiscountAmount")
    private BigDecimal discountAmount;

    @Column(name = "TotalAmount")
    private BigDecimal totalAmount;

    @Column(name = "Notes")
    private String notes;

    @Column(name = "IssuerID")
    private Integer issuerId;

    @Column(name = "PDFPath")
    private String pdfPath;

    // Relationships
    @OneToOne
    @JoinColumn(name = "TransactionID", insertable = false, updatable = false)
    private Transaction transaction;

    @ManyToOne
    @JoinColumn(name = "IssuerID", insertable = false, updatable = false)
    private Users issuer;

    // Constructors
    public Receipt() {}

    // Getters and Setters
    public Integer getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Integer receiptId) {
        this.receiptId = receiptId;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDate issuedDate) {
        this.issuedDate = issuedDate;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getIssuerId() {
        return issuerId;
    }

    public void setIssuerId(Integer issuerId) {
        this.issuerId = issuerId;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Users getIssuer() {
        return issuer;
    }

    public void setIssuer(Users issuer) {
        this.issuer = issuer;
    }
}