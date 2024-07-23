package bank;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/CustomerRegistrationServlet")
public class CustomerRegistrationServlet extends HttpServlet {
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fullName = request.getParameter("fullName");
        String address = request.getParameter("address");
        String mobileNo = request.getParameter("mobileNo");
        String email = request.getParameter("email");
        String accountType = request.getParameter("accountType");
        double initialBalance = Double.parseDouble(request.getParameter("initialBalance"));
        Date dob = Date.valueOf(request.getParameter("dob"));
        String idProof = request.getParameter("idProof");
        
        // Generate account number and temporary password
        int accountNo = generateAccountNumber();
        String temporaryPassword = generateTemporaryPassword();
        
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            // Create database connection
            conn = DatabaseConnection.getConnection();
            
            // Insert customer details into the database
            String insertQuery = "INSERT INTO customer (full_name, address, mobile_no, email, account_type, initial_balance, date_of_birth, id_proof, account_no, password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            pstmt = conn.prepareStatement(insertQuery);
            pstmt.setString(1, fullName);
            pstmt.setString(2, address);
            pstmt.setString(3, mobileNo);
            pstmt.setString(4, email);
            pstmt.setString(5, accountType);
            pstmt.setDouble(6, initialBalance);
            pstmt.setDate(7, dob);
            pstmt.setString(8, idProof);
            pstmt.setInt(9, accountNo);
            pstmt.setString(10, temporaryPassword);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                // Insert initial transaction record
                insertInitialTransaction(conn, accountNo, initialBalance);
                
                // Store account number in session
                HttpSession session = request.getSession();
                session.setAttribute("account_no", accountNo);
                
                // Redirect to registration success page
                response.sendRedirect("registration_success.jsp?accountNo=" + accountNo + "&password=" + temporaryPassword);
            } else {
                // Redirect to registration error page
                response.sendRedirect("registration_error.jsp");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Redirect to error page
            response.sendRedirect("error.jsp");
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void insertInitialTransaction(Connection conn, int accountNo, double initialBalance) throws SQLException {
        PreparedStatement initialTransactionStmt = null;
        try {
            // Insert initial transaction record for account creation
            String initialTransactionQuery = "INSERT INTO transaction (account_no, transaction_type, amount, transaction_date) VALUES (?, ?, ?, ?)";
            initialTransactionStmt = conn.prepareStatement(initialTransactionQuery);
            initialTransactionStmt.setInt(1, accountNo);
            initialTransactionStmt.setString(2, "Deposit"); // Assuming the creation of an account is considered a deposit
            initialTransactionStmt.setDouble(3, initialBalance); // Initial balance
            initialTransactionStmt.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis())); // Current timestamp
            initialTransactionStmt.executeUpdate();
        } finally {
            if (initialTransactionStmt != null) initialTransactionStmt.close();
        }
    }
    
    private int generateAccountNumber() {
        // Generate random account number
        return (int) (Math.random() * 900000) + 100000; // Generates a 6-digit number
    }
    
    private String generateTemporaryPassword() {
        // Generate random temporary password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }
}
