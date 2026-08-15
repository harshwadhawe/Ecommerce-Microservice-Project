import React from 'react';
import './Footer.css';

const Footer = () => {
  return (
    <footer className="footer">
      <div className="container">
        <div className="footer-content">
          <p>&copy; 2024 E-Commerce Application. All rights reserved.</p>
          <p>Built with React & Spring Boot Microservices</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;