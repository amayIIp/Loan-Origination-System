


module.exports = {

  
  
  
  
  content: [
    "./src/**/*.{html,ts}",       
    "./src/**/*.component.html",  
    "./src/**/*.component.ts",    
  ],

  
  theme: {
    extend: {
      
      
      colors: {
        
        'los-primary': '#1e3a5f',
        
        'los-accent': '#0d9488',
        
        'los-danger': '#dc2626',
        
        'los-warning': '#d97706',
        
        'los-surface': '#f8fafc',
      },

      
      
      fontFamily: {
        
        'los': ['Inter', 'system-ui', 'sans-serif'],
      },

      
      borderRadius: {
        'los-card': '0.75rem', 
      },

      
      boxShadow: {
        'los-card': '0 2px 8px 0 rgba(30, 58, 95, 0.08)',
        'los-modal': '0 8px 32px 0 rgba(30, 58, 95, 0.18)',
      },
    },
  },

  
  
  plugins: [
    
    
  ],
};
