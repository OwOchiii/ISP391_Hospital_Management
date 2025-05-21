package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Receipt")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReceiptID")
    private Integer receiptId;

    @Column(name = "TransactionID", nullable = false)
    private Integer transactionId;

    @Column(name = "ReceiptNumber", nullable = false, unique = true)
    private String receiptNumber;

    @Column(name = "IssuedDate", nullable = false, columnDefinition = "datetime DEFAULT GETDATE()")
    private LocalDateTime issuedDate;

    @Column(name = "Format", nullable = false, columnDefinition = "varchar(10) DEFAULT 'Digital'")
    private String format;

    @Column(name = "TaxAmount", nullable = false, precision = 10, scale = 2, columnDefinition = "decimal(10,2) DEFAULT 0")
    private BigDecimal taxAmount;

    @Column(name = "DiscountAmount", nullable = false, precision = 10, scale = 2, columnDefinition = "decimal(10,2) DEFAULT 0")
    private BigDecimal discountAmount;

    @Column(name = "TotalAmount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "Notes", length = Integer.MAX_VALUE)
    private String notes;

    @Column(name = "IssuerID", nullable = false)
    private Integer issuerId;

    @Column(name = "PDFPath")
    private String pdfPath;

    @OneToOne
    @JoinColumn(name = "TransactionID", insertable = false, updatable = false)
    private Transaction transaction;

    @ManyToOne
    @JoinColumn(name = "IssuerID", insertable = false, updatable = false)
    private Users issuer;
}
