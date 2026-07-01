




export interface ApplicantDto {
  
  name: string;
  
  email: string;
  
  phone: string;
  
  dob: string;
  
  address: string;
  
  nationalId: string;
}


export interface ApplicantResponseDto extends ApplicantDto {
  
  id: string;
}


export interface ApplicationDto {
  
  applicantId: string;
  
  amount: number;
  
  tenureMonths: number;
  
  purpose: string;
  
  monthlyIncome: number;
}


export interface ApplicationResponseDto extends ApplicationDto {
  
  id: string;
  
  status: string;
  
  decision?: DecisionResultDto;
}


export interface RuleResultDto {
  
  ruleName: string;
  
  passed: boolean;
  
  reason: string;
}


export interface DecisionResultDto {
  
  approved: boolean;
  
  riskScore: number;
  
  ruleResults: RuleResultDto[];
}
